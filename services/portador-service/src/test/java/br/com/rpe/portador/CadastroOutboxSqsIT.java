package br.com.rpe.portador;

import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.com.rpe.portador.adapters.out.persistence.OutboxEventEntity;
import br.com.rpe.portador.adapters.out.persistence.OutboxEventJpaRepository;
import br.com.rpe.portador.adapters.out.persistence.StatusOutboxEvent;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import java.time.Duration;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.localstack.LocalStackContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.utility.DockerImageName;
import software.amazon.awssdk.services.sqs.SqsAsyncClient;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageRequest;

/**
 * Prova de ponta a ponta do fluxo PO-06/PO-07 (PRD 9.1): cadastro -> outbox -> SQS, e a garantia de
 * que o cadastro nunca falha nem perde o evento quando o SQS está indisponível — o relay drena o
 * backlog automaticamente assim que a fila volta a existir.
 *
 * <p>"SQS indisponível" é simulado pela fila de destino ainda não existir no LocalStack ao
 * cadastrar (em vez de parar o container inteiro, o que reatribuiria a porta mapeada e complicaria
 * o teste sem mudar o que está sendo provado): do ponto de vista do código é o mesmo caminho de
 * exceção que uma falha de conexão real — {@code OutboxRelayUseCase} trata qualquer {@code
 * RuntimeException} de publicação de forma idêntica. Reproduzir literalmente "docker compose stop
 * localstack" fica para os scripts de caos manuais da Fase 5 (CLAUDE.md seção 11).
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
class CadastroOutboxSqsIT extends IntegrationTestBase {

  private static final String FILA = "cartao-emissao-queue";

  @Container
  static final LocalStackContainer LOCALSTACK =
      new LocalStackContainer(DockerImageName.parse("localstack/localstack:3"))
          .withServices(LocalStackContainer.Service.SQS);

  @RegisterExtension
  static WireMockExtension wireMock =
      WireMockExtension.newInstance().options(wireMockConfig().dynamicPort()).build();

  @DynamicPropertySource
  static void propriedades(DynamicPropertyRegistry registry) {
    registry.add(
        "spring.cloud.aws.sqs.endpoint",
        () -> LOCALSTACK.getEndpointOverride(LocalStackContainer.Service.SQS).toString());
    registry.add("spring.cloud.aws.region.static", LOCALSTACK::getRegion);
    registry.add("spring.cloud.aws.credentials.access-key", LOCALSTACK::getAccessKey);
    registry.add("spring.cloud.aws.credentials.secret-key", LOCALSTACK::getSecretKey);
    // FAIL (em vez do CREATE padrão): sem isso, o SqsTemplate criaria a fila sozinho no primeiro
    // envio, e o cenario de "fila ainda nao existe" nunca chegaria a falhar de verdade.
    registry.add("spring.cloud.aws.sqs.queue-not-found-strategy", () -> "FAIL");
    registry.add("rpe.portador.produto-client.base-url", wireMock::baseUrl);
    registry.add("rpe.portador.outbox.relay.fila", () -> FILA);
    registry.add("rpe.portador.outbox.relay.intervalo", () -> "1s");
    registry.add("rpe.portador.outbox.relay.backoff-base", () -> "300ms");
  }

  @Autowired private MockMvc mockMvc;
  @Autowired private SqsAsyncClient sqsAsyncClient;
  @Autowired private OutboxEventJpaRepository outboxEventJpaRepository;
  @Autowired private ObjectMapper objectMapper;

  private void stubProdutoAtivo(UUID produtoId) {
    wireMock.stubFor(
        get(urlEqualTo("/api/v1/produtos/" + produtoId))
            .willReturn(
                okJson(
                    """
                {"id":"%s","status":"ATIVO"}
                """
                        .formatted(produtoId))));
  }

  private UUID cadastrar(String cpf, UUID produtoId) throws Exception {
    String corpo =
        """
        {"nome":"Victor Rodrigues","cpf":"%s","dataNascimento":"2000-01-01","produtoId":"%s"}
        """
            .formatted(cpf, produtoId);
    String resposta =
        mockMvc
            .perform(
                post("/api/v1/portadores")
                    .with(jwt())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(corpo))
            .andExpect(status().isCreated())
            .andReturn()
            .getResponse()
            .getContentAsString();
    return UUID.fromString(objectMapper.readTree(resposta).get("id").asText());
  }

  private OutboxEventEntity buscarEventoDoPortador(UUID portadorId) {
    return outboxEventJpaRepository.findAll().stream()
        .filter(evento -> evento.getAggregateId().equals(portadorId))
        .findFirst()
        .orElseThrow();
  }

  @Test
  void naoDevePerderEventoComSqsForaEDeveDrenarEPublicarQuandoVoltar() throws Exception {
    UUID produtoId = UUID.randomUUID();
    stubProdutoAtivo(produtoId);

    // Fila de emissao ainda NAO existe no LocalStack neste ponto: simula SQS indisponivel.
    UUID portadorId = cadastrar("52998224725", produtoId);

    OutboxEventEntity eventoLogoAposCadastro = buscarEventoDoPortador(portadorId);
    assertThat(eventoLogoAposCadastro.getStatus()).isEqualTo(StatusOutboxEvent.PENDENTE);

    // Evento nao e perdido: o relay tenta, falha (fila ausente) e mantem PENDENTE com backoff,
    // nunca FALHOU antes de esgotar max-tentativas (default 5) - so provamos que ele nao desiste
    // cedo demais nem quebra o cadastro.
    await()
        .atMost(Duration.ofSeconds(5))
        .untilAsserted(
            () -> {
              OutboxEventEntity evento = buscarEventoDoPortador(portadorId);
              assertThat(evento.getStatus()).isEqualTo(StatusOutboxEvent.PENDENTE);
              assertThat(evento.getTentativas()).isGreaterThan(0);
            });

    // "SQS volta": a fila passa a existir.
    LOCALSTACK.execInContainer("awslocal", "sqs", "create-queue", "--queue-name", FILA);

    // Relay drena o backlog automaticamente, sem nova acao do cliente.
    await()
        .atMost(Duration.ofSeconds(10))
        .untilAsserted(
            () ->
                assertThat(buscarEventoDoPortador(portadorId).getStatus())
                    .isEqualTo(StatusOutboxEvent.PUBLICADO));

    String urlFila = sqsAsyncClient.getQueueUrl(r -> r.queueName(FILA)).get().queueUrl();
    var mensagens =
        sqsAsyncClient
            .receiveMessage(
                ReceiveMessageRequest.builder().queueUrl(urlFila).waitTimeSeconds(2).build())
            .get()
            .messages();
    assertThat(mensagens).isNotEmpty();
    JsonNode corpoMensagem = objectMapper.readTree(mensagens.get(0).body());
    assertThat(corpoMensagem.get("eventType").asText()).isEqualTo("CartaoEmissaoSolicitada");
    assertThat(corpoMensagem.get("data").get("portadorId").asText())
        .isEqualTo(portadorId.toString());
    assertThat(corpoMensagem.has("cpf")).isFalse();
  }
}

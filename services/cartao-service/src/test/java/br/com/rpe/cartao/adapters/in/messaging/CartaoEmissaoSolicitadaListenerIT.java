package br.com.rpe.cartao.adapters.in.messaging;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.mockito.Mockito.when;

import br.com.rpe.cartao.IntegrationTestBase;
import br.com.rpe.cartao.adapters.out.http.ProdutoHttpClient;
import br.com.rpe.cartao.application.port.out.CartaoRepositorio;
import br.com.rpe.cartao.application.port.out.ProdutoDto;
import br.com.rpe.cartao.application.port.out.StatusProdutoExterno;
import br.com.rpe.cartao.domain.exception.DependenciaIndisponivelException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Duration;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.localstack.LocalStackContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;
import software.amazon.awssdk.services.sqs.SqsAsyncClient;
import software.amazon.awssdk.services.sqs.model.MessageAttributeValue;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageRequest;
import software.amazon.awssdk.services.sqs.model.SendMessageRequest;

// So mocka o ProdutoHttpClient (fronteira HTTP), nao o ProdutoClient inteiro: o decorator de cache
// (ProdutoCacheAsideClient) roda de verdade, entao precisa de Redis real igual ao
// ProdutoAtualizadoListenerIT.
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@Testcontainers
class CartaoEmissaoSolicitadaListenerIT extends IntegrationTestBase {

  private static final String FILA = "cartao-emissao-queue";
  private static final String DLQ = "cartao-emissao-dlq";

  @Container @ServiceConnection
  static final GenericContainer<?> REDIS =
      new GenericContainer<>(DockerImageName.parse("redis:7-alpine")).withExposedPorts(6379);

  @Container
  static final LocalStackContainer LOCALSTACK =
      new LocalStackContainer(DockerImageName.parse("localstack/localstack:3"))
          .withServices(LocalStackContainer.Service.SQS);

  @DynamicPropertySource
  static void propriedades(DynamicPropertyRegistry registry) {
    registry.add(
        "spring.cloud.aws.sqs.endpoint",
        () -> LOCALSTACK.getEndpointOverride(LocalStackContainer.Service.SQS).toString());
    registry.add("spring.cloud.aws.region.static", LOCALSTACK::getRegion);
    registry.add("spring.cloud.aws.credentials.access-key", LOCALSTACK::getAccessKey);
    registry.add("spring.cloud.aws.credentials.secret-key", LOCALSTACK::getSecretKey);
    registry.add("rpe.cartao.mensageria.cartao-emissao-queue", () -> FILA);
    registry.add("rpe.cartao.mensageria.cartao-emissao-dlq", () -> DLQ);
  }

  @BeforeAll
  static void criarFilas() throws Exception {
    LOCALSTACK.execInContainer("awslocal", "sqs", "create-queue", "--queue-name", FILA);
    LOCALSTACK.execInContainer("awslocal", "sqs", "create-queue", "--queue-name", DLQ);
  }

  @Autowired private SqsAsyncClient sqsAsyncClient;
  @Autowired private ObjectMapper objectMapper;
  @Autowired private CartaoRepositorio cartaoRepositorio;
  @MockitoBean private ProdutoHttpClient produtoHttpClient;

  private String corpoValido(UUID eventId, UUID portadorId, UUID produtoId, String nomeImpresso) {
    try {
      return objectMapper.writeValueAsString(
          Map.of(
              "eventId",
              eventId.toString(),
              "eventType",
              "CartaoEmissaoSolicitada",
              "eventVersion",
              1,
              "correlationId",
              "corr-xyz",
              "data",
              Map.of(
                  "portadorId", portadorId.toString(),
                  "produtoId", produtoId.toString(),
                  "nomeImpresso", nomeImpresso)));
    } catch (Exception ex) {
      throw new RuntimeException(ex);
    }
  }

  private void enviar(String corpo) {
    String url = urlFila(FILA);
    sqsAsyncClient
        .sendMessage(
            SendMessageRequest.builder()
                .queueUrl(url)
                .messageBody(corpo)
                .messageAttributes(
                    Map.of(
                        "correlationId",
                        MessageAttributeValue.builder()
                            .dataType("String")
                            .stringValue("corr-xyz")
                            .build()))
                .build())
        .join();
  }

  private String urlFila(String nome) {
    return sqsAsyncClient.getQueueUrl(r -> r.queueName(nome)).join().queueUrl();
  }

  private java.util.List<software.amazon.awssdk.services.sqs.model.Message> receber(String fila) {
    return sqsAsyncClient
        .receiveMessage(
            ReceiveMessageRequest.builder()
                .queueUrl(urlFila(fila))
                .messageAttributeNames("All")
                .waitTimeSeconds(2)
                .build())
        .join()
        .messages();
  }

  @Test
  void deveEmitirCartaoQuandoProdutoAtivo() {
    UUID eventId = UUID.randomUUID();
    UUID portadorId = UUID.randomUUID();
    UUID produtoId = UUID.randomUUID();
    when(produtoHttpClient.buscarPorId(produtoId))
        .thenReturn(
            Optional.of(
                new ProdutoDto(produtoId, "Gold", "GOLD", "453201", StatusProdutoExterno.ATIVO)));

    enviar(corpoValido(eventId, portadorId, produtoId, "VICTOR RODRIGUES"));

    await()
        .atMost(Duration.ofSeconds(10))
        .untilAsserted(
            () ->
                assertThat(cartaoRepositorio.existePorPortadorEProduto(portadorId, produtoId))
                    .isTrue());
    assertThat(receber(DLQ)).isEmpty();
  }

  @Test
  void deveEnviarParaDlqQuandoProdutoInexistente() {
    UUID eventId = UUID.randomUUID();
    UUID portadorId = UUID.randomUUID();
    UUID produtoId = UUID.randomUUID();
    when(produtoHttpClient.buscarPorId(produtoId)).thenReturn(Optional.empty());

    enviar(corpoValido(eventId, portadorId, produtoId, "VICTOR RODRIGUES"));

    await().atMost(Duration.ofSeconds(10)).untilAsserted(() -> assertThat(receber(DLQ)).hasSize(1));
    assertThat(cartaoRepositorio.existePorPortadorEProduto(portadorId, produtoId)).isFalse();
  }

  @Test
  void deveEnviarParaDlqQuandoPayloadIlegivel() {
    enviar("{ isso nao e um json valido");

    await().atMost(Duration.ofSeconds(10)).untilAsserted(() -> assertThat(receber(DLQ)).hasSize(1));
  }

  @Test
  void deveEnviarParaDlqQuandoEventVersionIncompativel() throws Exception {
    UUID eventId = UUID.randomUUID();
    String corpo =
        objectMapper.writeValueAsString(
            Map.of(
                "eventId",
                eventId.toString(),
                "eventType",
                "CartaoEmissaoSolicitada",
                "eventVersion",
                99,
                "data",
                Map.of(
                    "portadorId", UUID.randomUUID().toString(),
                    "produtoId", UUID.randomUUID().toString(),
                    "nomeImpresso", "VICTOR")));

    enviar(corpo);

    await().atMost(Duration.ofSeconds(10)).untilAsserted(() -> assertThat(receber(DLQ)).hasSize(1));
  }

  @Test
  void naoDeveEnviarParaDlqQuandoErroForTransitorio() {
    UUID eventId = UUID.randomUUID();
    UUID portadorId = UUID.randomUUID();
    UUID produtoId = UUID.randomUUID();
    when(produtoHttpClient.buscarPorId(produtoId))
        .thenThrow(
            new DependenciaIndisponivelException(
                "Produto Service indisponível", Duration.ofSeconds(5)));

    enviar(corpoValido(eventId, portadorId, produtoId, "VICTOR RODRIGUES"));

    // Erro transitorio nunca e enviado manualmente para a DLQ pelo listener; so a redrive policy
    // da fila (maxReceiveCount, fora do escopo deste teste) move a mensagem apos exaurir as
    // tentativas. Aqui so provamos que o listener nao faz o envio direto.
    await()
        .during(Duration.ofSeconds(3))
        .atMost(Duration.ofSeconds(8))
        .untilAsserted(() -> assertThat(receber(DLQ)).isEmpty());
    assertThat(cartaoRepositorio.existePorPortadorEProduto(portadorId, produtoId)).isFalse();
  }
}

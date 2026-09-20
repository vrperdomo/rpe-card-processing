package br.com.rpe.portador.adapters.out.messaging;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.rpe.portador.IntegrationTestBase;
import br.com.rpe.portador.application.evento.EventoPendente;
import br.com.rpe.portador.config.OutboxRelayProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.UUID;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.localstack.LocalStackContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.utility.DockerImageName;
import software.amazon.awssdk.services.sqs.SqsAsyncClient;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageRequest;

// Contexto completo (inclui os repositorios JPA via component scan), por isso precisa de Postgres
// real via Testcontainers (IntegrationTestBase), alem do LocalStack para o SQS. Relay do outbox
// desligado: este teste chama o publisher diretamente, nao precisa do @Scheduled em background.
@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.NONE,
    properties = "rpe.portador.outbox.relay.ativo=false")
class SqsPublicadorEventosIT extends IntegrationTestBase {

  private static final String FILA = "cartao-emissao-queue";

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
    registry.add("rpe.portador.outbox.relay.fila", () -> FILA);
  }

  @BeforeAll
  static void criarFila() throws Exception {
    LOCALSTACK.execInContainer("awslocal", "sqs", "create-queue", "--queue-name", FILA);
  }

  @Autowired private SqsPublicadorEventos publicador;
  @Autowired private SqsAsyncClient sqsAsyncClient;
  @Autowired private OutboxRelayProperties properties;
  @Autowired private ObjectMapper objectMapper;

  @Test
  void devePublicarMensagemComPayloadEAtributosSqs() throws Exception {
    UUID portadorId = UUID.randomUUID();
    String payload =
        objectMapper.writeValueAsString(
            java.util.Map.of(
                "eventId",
                UUID.randomUUID().toString(),
                "eventType",
                "CartaoEmissaoSolicitada",
                "eventVersion",
                1,
                "correlationId",
                "corr-xyz",
                "data",
                java.util.Map.of("portadorId", portadorId.toString(), "nomeImpresso", "VICTOR")));
    EventoPendente evento =
        new EventoPendente(UUID.randomUUID(), portadorId, "CartaoEmissaoSolicitada", payload, 0);

    publicador.publicar(evento);

    String url = sqsAsyncClient.getQueueUrl(r -> r.queueName(properties.fila())).get().queueUrl();
    var mensagens =
        sqsAsyncClient
            .receiveMessage(
                ReceiveMessageRequest.builder()
                    .queueUrl(url)
                    .messageAttributeNames("All")
                    .waitTimeSeconds(5)
                    .build())
            .get()
            .messages();

    assertThat(mensagens).hasSize(1);
    var mensagem = mensagens.get(0);
    assertThat(mensagem.body()).isEqualTo(payload);
    assertThat(mensagem.messageAttributes().get("correlationId").stringValue())
        .isEqualTo("corr-xyz");
    assertThat(mensagem.messageAttributes().get("eventType").stringValue())
        .isEqualTo("CartaoEmissaoSolicitada");
    assertThat(mensagem.messageAttributes().get("eventVersion").stringValue()).isEqualTo("1");
  }
}

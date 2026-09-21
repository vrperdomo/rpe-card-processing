package br.com.rpe.cartao.adapters.in.messaging;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.mockito.Mockito.when;

import br.com.rpe.cartao.IntegrationTestBase;
import br.com.rpe.cartao.adapters.out.http.ProdutoHttpClient;
import br.com.rpe.cartao.application.port.out.CartaoRepositorio;
import br.com.rpe.cartao.application.port.out.EmissaoFalhaRepositorio;
import br.com.rpe.cartao.domain.exception.DependenciaIndisponivelException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Duration;
import java.util.Map;
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

/**
 * Prova, contra um SQS real (LocalStack), que o cabeçalho ApproximateReceiveCount chega ao listener
 * e que a falha transitória na ÚLTIMA entrega é registrada (issue #117). Com {@code
 * max-receive-count=1} a primeira entrega já é a última, o que evita esperar o visibility timeout
 * de uma reentrega real. A decisão "quando registrar" está no ListenerTest; aqui só se prova a
 * fiação com o SQS.
 */
@Testcontainers
@SpringBootTest
class CartaoEmissaoUltimaEntregaIT extends IntegrationTestBase {

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
    registry.add("rpe.cartao.mensageria.cartao-emissao-max-receive-count", () -> "1");
  }

  @BeforeAll
  static void criarFilas() throws Exception {
    LOCALSTACK.execInContainer("awslocal", "sqs", "create-queue", "--queue-name", FILA);
    LOCALSTACK.execInContainer("awslocal", "sqs", "create-queue", "--queue-name", DLQ);
  }

  @Autowired private SqsAsyncClient sqsAsyncClient;
  @Autowired private ObjectMapper objectMapper;
  @Autowired private CartaoRepositorio cartaoRepositorio;
  @Autowired private EmissaoFalhaRepositorio emissaoFalhaRepositorio;
  @MockitoBean private ProdutoHttpClient produtoHttpClient;

  @Test
  void deveRegistrarAFalhaQuandoErroTransitorioAcontecerNaUltimaEntrega() throws Exception {
    UUID portadorId = UUID.randomUUID();
    UUID produtoId = UUID.randomUUID();
    when(produtoHttpClient.buscarPorId(produtoId))
        .thenThrow(
            new DependenciaIndisponivelException(
                "Produto Service indisponível", Duration.ofSeconds(5)));
    String corpo =
        objectMapper.writeValueAsString(
            Map.of(
                "eventId",
                UUID.randomUUID().toString(),
                "eventType",
                "CartaoEmissaoSolicitada",
                "eventVersion",
                1,
                "data",
                Map.of(
                    "portadorId",
                    portadorId.toString(),
                    "produtoId",
                    produtoId.toString(),
                    "nomeImpresso",
                    "VICTOR RODRIGUES",
                    "criadoPor",
                    "admin")));
    String url = sqsAsyncClient.getQueueUrl(r -> r.queueName(FILA)).join().queueUrl();

    sqsAsyncClient.sendMessage(r -> r.queueUrl(url).messageBody(corpo)).join();

    await()
        .atMost(Duration.ofSeconds(15))
        .untilAsserted(
            () -> {
              var falha = emissaoFalhaRepositorio.buscarPorPortadorId(portadorId);
              assertThat(falha).isPresent();
              assertThat(falha.get().motivo()).startsWith("Tentativas de emissão esgotadas");
              assertThat(falha.get().criadoPor()).isEqualTo("admin");
            });
    assertThat(cartaoRepositorio.existePorPortadorEProduto(portadorId, produtoId)).isFalse();
  }
}

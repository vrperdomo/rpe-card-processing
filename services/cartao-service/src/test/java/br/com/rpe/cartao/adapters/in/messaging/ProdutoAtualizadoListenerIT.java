package br.com.rpe.cartao.adapters.in.messaging;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import br.com.rpe.cartao.IntegrationTestBase;
import br.com.rpe.cartao.adapters.out.cache.ProdutoCacheEntry;
import java.time.Duration;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.localstack.LocalStackContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;
import software.amazon.awssdk.services.sqs.SqsAsyncClient;
import software.amazon.awssdk.services.sqs.model.MessageAttributeValue;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageRequest;
import software.amazon.awssdk.services.sqs.model.SendMessageRequest;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@Testcontainers
class ProdutoAtualizadoListenerIT extends IntegrationTestBase {

  private static final String FILA = "produto-eventos-queue";
  private static final String DLQ = "produto-eventos-dlq";

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
  }

  @BeforeAll
  static void criarFilas() throws Exception {
    LOCALSTACK.execInContainer("awslocal", "sqs", "create-queue", "--queue-name", DLQ);
    LOCALSTACK.execInContainer("awslocal", "sqs", "create-queue", "--queue-name", FILA);
  }

  @Autowired private SqsAsyncClient sqsAsyncClient;
  @Autowired private RedisTemplate<String, ProdutoCacheEntry> redisTemplate;

  private String chave(UUID id) {
    return "produto:v1:" + id;
  }

  private String urlDaFila(String nome) {
    return sqsAsyncClient.getQueueUrl(r -> r.queueName(nome)).join().queueUrl();
  }

  private void popularCache(UUID id) {
    redisTemplate
        .opsForValue()
        .set(
            chave(id),
            new ProdutoCacheEntry(
                new br.com.rpe.cartao.application.port.out.ProdutoDto(
                    id,
                    "Gold",
                    "GOLD",
                    "453201",
                    br.com.rpe.cartao.application.port.out.StatusProdutoExterno.ATIVO)),
            Duration.ofMinutes(10));
  }

  @Test
  void deveEvictarCacheAoReceberEventoValido() {
    UUID produtoId = UUID.randomUUID();
    popularCache(produtoId);
    String corpo =
        """
        {"eventType":"ProdutoAtualizado","eventVersion":1,"data":{"produtoId":"%s"}}
        """
            .formatted(produtoId);

    sqsAsyncClient
        .sendMessage(
            SendMessageRequest.builder()
                .queueUrl(urlDaFila(FILA))
                .messageBody(corpo)
                .messageAttributes(
                    Map.of(
                        "correlationId",
                        MessageAttributeValue.builder()
                            .dataType("String")
                            .stringValue(UUID.randomUUID().toString())
                            .build()))
                .build())
        .join();

    await()
        .atMost(Duration.ofSeconds(10))
        .untilAsserted(() -> assertThat(redisTemplate.hasKey(chave(produtoId))).isFalse());
  }

  @Test
  void deveEnviarParaDlqQuandoMensagemIlegivel() {
    sqsAsyncClient
        .sendMessage(
            SendMessageRequest.builder()
                .queueUrl(urlDaFila(FILA))
                .messageBody("{ nao-json")
                .build())
        .join();

    await()
        .atMost(Duration.ofSeconds(10))
        .untilAsserted(
            () -> {
              var mensagens =
                  sqsAsyncClient
                      .receiveMessage(
                          ReceiveMessageRequest.builder()
                              .queueUrl(urlDaFila(DLQ))
                              .waitTimeSeconds(1)
                              .build())
                      .join()
                      .messages();
              assertThat(mensagens).isNotEmpty();
            });
  }

  @Test
  void deveEnviarParaDlqQuandoEventVersionIncompativel() {
    UUID produtoId = UUID.randomUUID();
    String corpo =
        """
        {"eventType":"ProdutoAtualizado","eventVersion":99,"data":{"produtoId":"%s"}}
        """
            .formatted(produtoId);

    sqsAsyncClient
        .sendMessage(
            SendMessageRequest.builder().queueUrl(urlDaFila(FILA)).messageBody(corpo).build())
        .join();

    await()
        .atMost(Duration.ofSeconds(10))
        .untilAsserted(
            () -> {
              var mensagens =
                  sqsAsyncClient
                      .receiveMessage(
                          ReceiveMessageRequest.builder()
                              .queueUrl(urlDaFila(DLQ))
                              .waitTimeSeconds(1)
                              .build())
                      .join()
                      .messages();
              assertThat(mensagens).isNotEmpty();
            });
  }
}

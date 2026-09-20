package br.com.rpe.produto.adapters.out.messaging;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.rpe.produto.IntegrationTestBase;
import br.com.rpe.produto.application.port.out.ProdutoRepositorio;
import br.com.rpe.produto.application.usecase.AlterarStatusProdutoUseCase;
import br.com.rpe.produto.config.MensageriaProperties;
import br.com.rpe.produto.domain.CategoriaProduto;
import br.com.rpe.produto.domain.Produto;
import br.com.rpe.produto.domain.StatusProduto;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
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

// Contexto completo (repositorios JPA via component scan), por isso precisa de Postgres real
// (IntegrationTestBase) alem do LocalStack para o SQS. Prova o comportamento AFTER_COMMIT de
// ponta a ponta: chama o use case real (nao o publisher isolado) para garantir que a mensagem so
// sai depois que a transacao de cancelamento do produto realmente commitou.
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
class ProdutoAtualizadoPublisherIT extends IntegrationTestBase {

  private static final String FILA = "produto-eventos-queue";

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
    registry.add("rpe.produto.mensageria.produto-eventos-queue", () -> FILA);
  }

  @BeforeAll
  static void criarFila() throws Exception {
    LOCALSTACK.execInContainer("awslocal", "sqs", "create-queue", "--queue-name", FILA);
  }

  @Autowired private AlterarStatusProdutoUseCase alterarStatusProdutoUseCase;
  @Autowired private ProdutoRepositorio produtoRepositorio;
  @Autowired private SqsAsyncClient sqsAsyncClient;
  @Autowired private MensageriaProperties properties;
  @Autowired private ObjectMapper objectMapper;

  @Test
  void devePublicarProdutoAtualizadoApenasAposCommitDoCancelamento() throws Exception {
    Produto produto =
        produtoRepositorio.salvar(
            Produto.criar("Gold", "descricao", CategoriaProduto.GOLD, "123456", Instant.now()));

    alterarStatusProdutoUseCase.executar(produto.getId(), StatusProduto.CANCELADO, "corr-abc");

    String url =
        sqsAsyncClient
            .getQueueUrl(r -> r.queueName(properties.produtoEventosQueue()))
            .get()
            .queueUrl();
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
    JsonNode corpo = objectMapper.readTree(mensagem.body());
    assertThat(corpo.path("eventType").asText()).isEqualTo("ProdutoAtualizado");
    assertThat(corpo.path("eventVersion").asInt()).isEqualTo(1);
    assertThat(corpo.path("data").path("produtoId").asText()).isEqualTo(produto.getId().toString());
    assertThat(mensagem.messageAttributes().get("correlationId").stringValue())
        .isEqualTo("corr-abc");
    assertThat(mensagem.messageAttributes().get("eventType").stringValue())
        .isEqualTo("ProdutoAtualizado");
    assertThat(mensagem.messageAttributes().get("eventVersion").stringValue()).isEqualTo("1");
  }
}

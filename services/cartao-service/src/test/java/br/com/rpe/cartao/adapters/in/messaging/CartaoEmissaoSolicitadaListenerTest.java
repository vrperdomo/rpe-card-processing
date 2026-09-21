package br.com.rpe.cartao.adapters.in.messaging;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import br.com.rpe.cartao.application.usecase.EmitirCartaoUseCase;
import br.com.rpe.cartao.application.usecase.RegistrarFalhaEmissaoUseCase;
import br.com.rpe.cartao.config.MensageriaProperties;
import br.com.rpe.cartao.domain.exception.DependenciaIndisponivelException;
import br.com.rpe.cartao.domain.exception.RegraNegocioException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.awspring.cloud.sqs.operations.SqsTemplate;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.time.Duration;
import java.util.UUID;
import java.util.function.Consumer;
import org.junit.jupiter.api.Test;

/**
 * Cobre a decisão de QUANDO registrar a falha de emissão (issue #117): definitiva na hora,
 * transitória só na última entrega do SQS. O caminho real pelo SQS está no ListenerIT.
 */
class CartaoEmissaoSolicitadaListenerTest {

  private static final int MAX_RECEIVE_COUNT = 3;

  private final EmitirCartaoUseCase emitir = mock(EmitirCartaoUseCase.class);
  private final RegistrarFalhaEmissaoUseCase registrarFalha =
      mock(RegistrarFalhaEmissaoUseCase.class);
  private final SqsTemplate sqsTemplate = mock(SqsTemplate.class);
  private final SimpleMeterRegistry meterRegistry = new SimpleMeterRegistry();
  private final CartaoEmissaoSolicitadaListener listener =
      new CartaoEmissaoSolicitadaListener(
          emitir,
          registrarFalha,
          sqsTemplate,
          new ObjectMapper(),
          new MensageriaProperties("pq", "pd", "cq", "cd", MAX_RECEIVE_COUNT, true),
          meterRegistry);

  private final UUID eventId = UUID.randomUUID();
  private final UUID portadorId = UUID.randomUUID();
  private final UUID produtoId = UUID.randomUUID();

  private String corpo(int eventVersion, String criadoPor) {
    String dono = criadoPor == null ? "" : ",\"criadoPor\":\"%s\"".formatted(criadoPor);
    return """
        {"eventId":"%s","eventType":"CartaoEmissaoSolicitada","eventVersion":%d,
         "data":{"portadorId":"%s","produtoId":"%s","nomeImpresso":"VICTOR"%s}}
        """
        .formatted(eventId, eventVersion, portadorId, produtoId, dono);
  }

  private void falharEmitirCom(RuntimeException ex) {
    doThrow(ex)
        .when(emitir)
        .executar(eq(eventId), eq(portadorId), eq(produtoId), anyString(), any());
  }

  @Test
  void deveRegistrarFalhaDefinitivaEEnviarParaDlqQuandoProdutoInvalido() {
    falharEmitirCom(new RegraNegocioException("Produto inexistente ou não ATIVO"));

    listener.ouvir(corpo(1, "admin"), "corr", "1");

    verify(registrarFalha)
        .registrarDefinitiva(portadorId, produtoId, "admin", "Produto inexistente ou não ATIVO");
    verify(sqsTemplate).send(any(Consumer.class));
  }

  @Test
  void deveRegistrarFalhaDefinitivaQuandoSchemaIncompativelMasHaPortadorEProduto() {
    listener.ouvir(corpo(99, "admin"), "corr", "1");

    verify(registrarFalha)
        .registrarDefinitiva(
            eq(portadorId),
            eq(produtoId),
            eq("admin"),
            eq("Mensagem de emissão com schema ou versão incompatível"));
    verify(sqsTemplate).send(any(Consumer.class));
  }

  @Test
  void naoDeveRegistrarFalhaQuandoPayloadIlegivelPoisNaoHaPortador() {
    listener.ouvir("{ isso nao e json", "corr", "1");

    verifyNoInteractions(registrarFalha);
    verify(sqsTemplate).send(any(Consumer.class));
  }

  @Test
  void deveRegistrarTentativasEsgotadasERelancarNaUltimaEntrega() {
    DependenciaIndisponivelException erro =
        new DependenciaIndisponivelException("Produto fora", Duration.ofSeconds(5));
    falharEmitirCom(erro);

    assertThatThrownBy(() -> listener.ouvir(corpo(1, "admin"), "corr", "3")).isSameAs(erro);

    verify(registrarFalha).registrarTentativasEsgotadas(portadorId, produtoId, "admin");
    verify(sqsTemplate, never()).send(any(Consumer.class));
  }

  @Test
  void naoDeveRegistrarNemEnviarParaDlqAntesDaUltimaEntrega() {
    falharEmitirCom(new DependenciaIndisponivelException("Produto fora", Duration.ofSeconds(5)));

    assertThatThrownBy(() -> listener.ouvir(corpo(1, "admin"), "corr", "2"))
        .isInstanceOf(DependenciaIndisponivelException.class);

    verifyNoInteractions(registrarFalha);
    verify(sqsTemplate, never()).send(any(Consumer.class));
  }

  @Test
  void naoDeveRegistrarQuandoOCabecalhoDeEntregasNaoVem() {
    falharEmitirCom(new DependenciaIndisponivelException("Produto fora", Duration.ofSeconds(5)));

    assertThatThrownBy(() -> listener.ouvir(corpo(1, "admin"), "corr", null))
        .isInstanceOf(DependenciaIndisponivelException.class);

    verifyNoInteractions(registrarFalha);
  }

  @Test
  void naoDeveRegistrarQuandoOCabecalhoDeEntregasEInvalido() {
    falharEmitirCom(new DependenciaIndisponivelException("Produto fora", Duration.ofSeconds(5)));

    assertThatThrownBy(() -> listener.ouvir(corpo(1, "admin"), "corr", "abc"))
        .isInstanceOf(DependenciaIndisponivelException.class);

    verifyNoInteractions(registrarFalha);
  }

  @Test
  void falhaAoRegistrarNaoDeveImpedirOEnvioParaDlq() {
    falharEmitirCom(new RegraNegocioException("Produto inexistente ou não ATIVO"));
    doThrow(new IllegalStateException("banco fora"))
        .when(registrarFalha)
        .registrarDefinitiva(any(), any(), any(), anyString());

    listener.ouvir(corpo(1, "admin"), "corr", "1");

    verify(sqsTemplate).send(any(Consumer.class));
    assertThat(meterRegistry.counter("cartao.dlq.enviados").count()).isEqualTo(1.0);
  }

  @Test
  void falhaAoRegistrarNaUltimaEntregaNaoDeveTrocarAExcecaoOriginal() {
    DependenciaIndisponivelException erro =
        new DependenciaIndisponivelException("Produto fora", Duration.ofSeconds(5));
    falharEmitirCom(erro);
    doThrow(new IllegalStateException("banco fora"))
        .when(registrarFalha)
        .registrarTentativasEsgotadas(any(), any(), any());

    assertThatThrownBy(() -> listener.ouvir(corpo(1, "admin"), "corr", "3")).isSameAs(erro);
  }

  @Test
  void naoDeveRegistrarQuandoAEmissaoDaCerto() {
    listener.ouvir(corpo(1, "admin"), "corr", "3");

    verifyNoInteractions(registrarFalha);
    verify(sqsTemplate, never()).send(any(Consumer.class));
  }
}

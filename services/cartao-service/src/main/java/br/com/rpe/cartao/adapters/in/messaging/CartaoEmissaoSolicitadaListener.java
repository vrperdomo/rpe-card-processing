package br.com.rpe.cartao.adapters.in.messaging;

import br.com.rpe.cartao.application.usecase.EmitirCartaoUseCase;
import br.com.rpe.cartao.application.usecase.RegistrarFalhaEmissaoUseCase;
import br.com.rpe.cartao.config.MensageriaProperties;
import br.com.rpe.cartao.domain.exception.RegraNegocioException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.awspring.cloud.sqs.annotation.SqsListener;
import io.awspring.cloud.sqs.listener.SqsHeaders;
import io.awspring.cloud.sqs.operations.SqsTemplate;
import io.micrometer.core.instrument.MeterRegistry;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

/**
 * Consome CartaoEmissaoSolicitada e emite o cartao (PRD 9.2, CA-01). Erros sao classificados em
 * dois grupos (CLAUDE.md secao 6.4):
 *
 * <ul>
 *   <li><b>Transitorio</b> — Produto Service indisponivel (circuito aberto/timeout): o metodo lanca
 *       a excecao adiante, o container nao confirma (ON_SUCCESS) e o SQS reentrega ate {@code
 *       maxReceiveCount}, quando entao a propria fila move a mensagem para a DLQ via redrive policy
 *       (nenhum envio manual necessario). Na ULTIMA entrega (ApproximateReceiveCount ==
 *       maxReceiveCount) a falha e registrada antes de relancar, porque a mensagem nao volta mais
 *       para este listener.
 *   <li><b>Definitivo</b> — payload ilegivel, schema/versao incompativel, ou produto
 *       inexistente/CANCELADO: a mensagem vai direto para a DLQ com o motivo, e o metodo retorna
 *       normalmente (ack), sem redelivery. Exceto o payload ilegivel (sem portadorId nao ha a quem
 *       atribuir a falha), a falha e registrada para o Portador poder exibir FALHOU (issue #117).
 * </ul>
 */
@Component
public class CartaoEmissaoSolicitadaListener {

  private static final Logger log = LoggerFactory.getLogger(CartaoEmissaoSolicitadaListener.class);
  private static final String MDC_CORRELATION_ID = "correlationId";
  private static final String MDC_EVENT_ID = "eventId";
  private static final String MOTIVO_SCHEMA =
      "Mensagem de emissão com schema ou versão incompatível";
  private static final String MOTIVO_GENERICO = "Falha definitiva na emissão do cartão";

  private final EmitirCartaoUseCase useCase;
  private final RegistrarFalhaEmissaoUseCase registrarFalhaUseCase;
  private final SqsTemplate sqsTemplate;
  private final ObjectMapper objectMapper;
  private final MensageriaProperties properties;
  private final MeterRegistry meterRegistry;

  public CartaoEmissaoSolicitadaListener(
      EmitirCartaoUseCase useCase,
      RegistrarFalhaEmissaoUseCase registrarFalhaUseCase,
      SqsTemplate sqsTemplate,
      ObjectMapper objectMapper,
      MensageriaProperties properties,
      MeterRegistry meterRegistry) {
    this.useCase = useCase;
    this.registrarFalhaUseCase = registrarFalhaUseCase;
    this.sqsTemplate = sqsTemplate;
    this.objectMapper = objectMapper;
    this.properties = properties;
    this.meterRegistry = meterRegistry;
  }

  @SqsListener(
      queueNames = "${rpe.cartao.mensageria.cartao-emissao-queue}",
      acknowledgementMode = "ON_SUCCESS")
  public void ouvir(
      String corpo,
      @Header(value = "correlationId", required = false) String correlationId,
      @Header(
              value = SqsHeaders.MessageSystemAttributes.SQS_APPROXIMATE_RECEIVE_COUNT,
              required = false)
          String entregas) {
    MDC.put(MDC_CORRELATION_ID, correlationId != null ? correlationId : "desconhecido");
    try {
      processar(corpo, ehUltimaEntrega(entregas));
    } finally {
      MDC.remove(MDC_CORRELATION_ID);
    }
  }

  private void processar(String corpo, boolean ultimaEntrega) {
    CartaoEmissaoSolicitadaMensagem mensagem = tentarDesserializar(corpo);
    if (mensagem == null) {
      enviarParaDlq(corpo, "payload_ilegivel");
      return;
    }
    if (!mensagem.valida()) {
      log.warn("Mensagem CartaoEmissaoSolicitada com schema ou eventVersion incompatível");
      registrarFalhaDefinitiva(mensagem, MOTIVO_SCHEMA);
      enviarParaDlq(corpo, "schema_incompativel");
      return;
    }
    MDC.put(MDC_EVENT_ID, mensagem.eventId().toString());
    try {
      emitir(mensagem);
    } catch (RegraNegocioException ex) {
      log.warn("Erro definitivo ao emitir cartão: {}", ex.getMessage());
      registrarFalhaDefinitiva(mensagem, motivoDe(ex));
      enviarParaDlq(corpo, ex.getMessage());
    } catch (DataIntegrityViolationException ex) {
      // Corrida entre duas entregas da mesma mensagem (ou eventos distintos para o mesmo par
      // portador+produto): a constraint UNIQUE do banco e quem barra, tratado como idempotente.
      log.info("Constraint de unicidade violada ao emitir cartão, tratando como já processado");
    } catch (RuntimeException ex) {
      // Transitorio: nao confirma (relanca) para o SQS reentregar. Se esta foi a ultima entrega, a
      // proxima acao do SQS e mover a mensagem para a DLQ; registra a falha antes que isso ocorra.
      if (ultimaEntrega) {
        registrarFalhaEsgotada(mensagem);
      }
      throw ex;
    } finally {
      MDC.remove(MDC_EVENT_ID);
    }
  }

  private void emitir(CartaoEmissaoSolicitadaMensagem mensagem) {
    useCase.executar(
        mensagem.eventId(),
        mensagem.data().portadorId(),
        mensagem.data().produtoId(),
        mensagem.data().nomeImpresso(),
        mensagem.data().criadoPor());
  }

  // maxReceiveCount e a ultima entrega que o SQS faz: na seguinte a mensagem ja vai para a DLQ. O
  // valor configurado precisa refletir o RedrivePolicy da fila (infra/localstack/init-queues.sh);
  // se for MAIOR que o real a falha nao e registrada (o portador segue PENDENTE, degrada seguro);
  // se for MENOR e uma nova tentativa der certo, a emissao apaga a falha registrada.
  private boolean ehUltimaEntrega(String entregas) {
    if (entregas == null) {
      return false;
    }
    try {
      return Integer.parseInt(entregas.trim()) >= properties.cartaoEmissaoMaxReceiveCount();
    } catch (NumberFormatException ex) {
      return false;
    }
  }

  private void registrarFalhaDefinitiva(CartaoEmissaoSolicitadaMensagem mensagem, String motivo) {
    CartaoEmissaoSolicitadaMensagem.Dados dados = mensagem.data();
    if (!temPortadorEProduto(dados)) {
      return;
    }
    protegido(
        () ->
            registrarFalhaUseCase.registrarDefinitiva(
                dados.portadorId(), dados.produtoId(), dados.criadoPor(), motivo));
  }

  private void registrarFalhaEsgotada(CartaoEmissaoSolicitadaMensagem mensagem) {
    CartaoEmissaoSolicitadaMensagem.Dados dados = mensagem.data();
    protegido(
        () ->
            registrarFalhaUseCase.registrarTentativasEsgotadas(
                dados.portadorId(), dados.produtoId(), dados.criadoPor()));
  }

  private static boolean temPortadorEProduto(CartaoEmissaoSolicitadaMensagem.Dados dados) {
    return dados != null && dados.portadorId() != null && dados.produtoId() != null;
  }

  private static String motivoDe(RegraNegocioException ex) {
    return ex.getMessage() != null && !ex.getMessage().isBlank()
        ? ex.getMessage()
        : MOTIVO_GENERICO;
  }

  // Registrar a falha e um efeito colateral informativo: se o banco falhar aqui, o fluxo principal
  // (DLQ ou relancar para o SQS reentregar) precisa seguir intacto. A falha e logada com stack.
  private void protegido(Runnable acao) {
    try {
      acao.run();
    } catch (RuntimeException ex) {
      log.error("Não foi possível registrar a falha de emissão do cartão", ex);
    }
  }

  private CartaoEmissaoSolicitadaMensagem tentarDesserializar(String corpo) {
    try {
      return objectMapper.readValue(corpo, CartaoEmissaoSolicitadaMensagem.class);
    } catch (JsonProcessingException ex) {
      log.warn("Mensagem CartaoEmissaoSolicitada ilegível", ex);
      return null;
    }
  }

  private void enviarParaDlq(String corpo, String motivo) {
    sqsTemplate.send(
        to ->
            to.queue(properties.cartaoEmissaoDlq())
                .payload(corpo)
                .headers(Map.of("erro-motivo", motivo)));
    meterRegistry.counter("cartao.dlq.enviados").increment();
  }
}

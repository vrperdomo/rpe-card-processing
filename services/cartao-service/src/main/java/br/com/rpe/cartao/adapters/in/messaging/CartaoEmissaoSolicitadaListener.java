package br.com.rpe.cartao.adapters.in.messaging;

import br.com.rpe.cartao.application.usecase.EmitirCartaoUseCase;
import br.com.rpe.cartao.config.MensageriaProperties;
import br.com.rpe.cartao.domain.exception.RegraNegocioException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.awspring.cloud.sqs.annotation.SqsListener;
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
 *       (nenhum envio manual necessario).
 *   <li><b>Definitivo</b> — payload ilegivel, schema/versao incompativel, ou produto
 *       inexistente/CANCELADO: a mensagem vai direto para a DLQ com o motivo, e o metodo retorna
 *       normalmente (ack), sem redelivery.
 * </ul>
 */
@Component
public class CartaoEmissaoSolicitadaListener {

  private static final Logger log = LoggerFactory.getLogger(CartaoEmissaoSolicitadaListener.class);
  private static final String MDC_CORRELATION_ID = "correlationId";

  private final EmitirCartaoUseCase useCase;
  private final SqsTemplate sqsTemplate;
  private final ObjectMapper objectMapper;
  private final MensageriaProperties properties;
  private final MeterRegistry meterRegistry;

  public CartaoEmissaoSolicitadaListener(
      EmitirCartaoUseCase useCase,
      SqsTemplate sqsTemplate,
      ObjectMapper objectMapper,
      MensageriaProperties properties,
      MeterRegistry meterRegistry) {
    this.useCase = useCase;
    this.sqsTemplate = sqsTemplate;
    this.objectMapper = objectMapper;
    this.properties = properties;
    this.meterRegistry = meterRegistry;
  }

  @SqsListener(
      queueNames = "${rpe.cartao.mensageria.cartao-emissao-queue}",
      acknowledgementMode = "ON_SUCCESS")
  public void ouvir(
      String corpo, @Header(value = "correlationId", required = false) String correlationId) {
    MDC.put(MDC_CORRELATION_ID, correlationId != null ? correlationId : "desconhecido");
    try {
      processar(corpo);
    } finally {
      MDC.remove(MDC_CORRELATION_ID);
    }
  }

  private void processar(String corpo) {
    CartaoEmissaoSolicitadaMensagem mensagem = tentarDesserializar(corpo);
    if (mensagem == null) {
      enviarParaDlq(corpo, "payload_ilegivel");
      return;
    }
    if (!mensagem.valida()) {
      log.warn("Mensagem CartaoEmissaoSolicitada com schema ou eventVersion incompatível");
      enviarParaDlq(corpo, "schema_incompativel");
      return;
    }
    try {
      useCase.executar(
          mensagem.eventId(),
          mensagem.data().portadorId(),
          mensagem.data().produtoId(),
          mensagem.data().nomeImpresso());
    } catch (RegraNegocioException ex) {
      log.warn("Erro definitivo ao emitir cartão: {}", ex.getMessage());
      enviarParaDlq(corpo, ex.getMessage());
    } catch (DataIntegrityViolationException ex) {
      // Corrida entre duas entregas da mesma mensagem (ou eventos distintos para o mesmo par
      // portador+produto): a constraint UNIQUE do banco e quem barra, tratado como idempotente.
      log.info("Constraint de unicidade violada ao emitir cartão, tratando como já processado");
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

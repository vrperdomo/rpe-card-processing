package br.com.rpe.cartao.adapters.in.messaging;

import br.com.rpe.cartao.application.usecase.EvictarCacheProdutoUseCase;
import br.com.rpe.cartao.config.MensageriaProperties;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.awspring.cloud.sqs.annotation.SqsListener;
import io.awspring.cloud.sqs.operations.SqsTemplate;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

/**
 * Consome ProdutoAtualizado e evicta o cache correspondente no Redis (issue #27, PRD decisão 19.3).
 * Mensagens ilegíveis ou com schema/versão incompatível são erro definitivo: vão direto para a DLQ,
 * sem redelivery. Falha ao evictar por Redis indisponível não é propagada como erro — o TTL do
 * cache-aside é a rede de segurança documentada na ADR-003.
 */
@Component
public class ProdutoAtualizadoListener {

  private static final Logger log = LoggerFactory.getLogger(ProdutoAtualizadoListener.class);
  private static final String MDC_CORRELATION_ID = "correlationId";

  private final EvictarCacheProdutoUseCase useCase;
  private final SqsTemplate sqsTemplate;
  private final ObjectMapper objectMapper;
  private final MensageriaProperties properties;

  public ProdutoAtualizadoListener(
      EvictarCacheProdutoUseCase useCase,
      SqsTemplate sqsTemplate,
      ObjectMapper objectMapper,
      MensageriaProperties properties) {
    this.useCase = useCase;
    this.sqsTemplate = sqsTemplate;
    this.objectMapper = objectMapper;
    this.properties = properties;
  }

  @SqsListener(
      queueNames = "${rpe.cartao.mensageria.produto-eventos-queue}",
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
    ProdutoAtualizadoMensagem mensagem = tentarDesserializar(corpo);
    if (mensagem == null) {
      enviarParaDlq(corpo, "payload_ilegivel");
      return;
    }
    if (!mensagem.valida()) {
      log.warn("Mensagem ProdutoAtualizado com schema ou eventVersion incompatível");
      enviarParaDlq(corpo, "schema_incompativel");
      return;
    }
    useCase.executar(mensagem.data().produtoId());
  }

  private ProdutoAtualizadoMensagem tentarDesserializar(String corpo) {
    try {
      return objectMapper.readValue(corpo, ProdutoAtualizadoMensagem.class);
    } catch (JsonProcessingException ex) {
      log.warn("Mensagem ProdutoAtualizado ilegível", ex);
      return null;
    }
  }

  private void enviarParaDlq(String corpo, String motivo) {
    sqsTemplate.send(
        to ->
            to.queue(properties.produtoEventosDlq())
                .payload(corpo)
                .headers(Map.of("erro-motivo", motivo)));
  }
}

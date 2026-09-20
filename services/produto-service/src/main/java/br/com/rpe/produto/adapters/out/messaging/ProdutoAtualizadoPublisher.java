package br.com.rpe.produto.adapters.out.messaging;

import br.com.rpe.produto.adapters.out.messaging.dto.ProdutoAtualizadoPayload;
import br.com.rpe.produto.application.evento.ProdutoAtualizadoEvento;
import br.com.rpe.produto.config.MensageriaProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.awspring.cloud.sqs.operations.SqsTemplate;
import java.time.Clock;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Publica ProdutoAtualizado no SQS somente apos o commit da transacao que alterou o produto
 * (decisao 19.3 do PRD / ADR-003). Nao usa Transactional Outbox: aqui a falha de publicacao e
 * tolerada porque o TTL do cache-aside no Cartao (issue #24/#26) e a rede de seguranca — ao
 * contrario da emissao de cartao no Portador, onde a perda do evento e inaceitavel.
 */
@Component
public class ProdutoAtualizadoPublisher {

  private static final Logger log = LoggerFactory.getLogger(ProdutoAtualizadoPublisher.class);

  private final SqsTemplate sqsTemplate;
  private final MensageriaProperties properties;
  private final ObjectMapper objectMapper;
  private final Clock clock;

  public ProdutoAtualizadoPublisher(
      SqsTemplate sqsTemplate,
      MensageriaProperties properties,
      ObjectMapper objectMapper,
      Clock clock) {
    this.sqsTemplate = sqsTemplate;
    this.properties = properties;
    this.objectMapper = objectMapper;
    this.clock = clock;
  }

  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
  public void aoConfirmarTransacao(ProdutoAtualizadoEvento evento) {
    ProdutoAtualizadoPayload payload =
        new ProdutoAtualizadoPayload(
            UUID.randomUUID(),
            ProdutoAtualizadoPayload.EVENT_TYPE,
            ProdutoAtualizadoPayload.EVENT_VERSION,
            Instant.now(clock),
            evento.correlationId(),
            new ProdutoAtualizadoPayload.Dados(evento.produtoId()));
    try {
      String corpo = objectMapper.writeValueAsString(payload);
      sqsTemplate.send(
          to ->
              to.queue(properties.produtoEventosQueue())
                  .payload(corpo)
                  .headers(
                      Map.of(
                          "correlationId",
                              payload.correlationId() != null ? payload.correlationId() : "",
                          "eventType", payload.eventType(),
                          "eventVersion", String.valueOf(payload.eventVersion()))));
    } catch (Exception ex) {
      // Falha ao publicar aqui nao reverte a alteracao do produto (ja commitada) nem propaga pro
      // chamador HTTP; o TTL do cache no Cartao cobre a janela de inconsistencia (ADR-003).
      log.warn("Falha ao publicar ProdutoAtualizado para produto {}", evento.produtoId(), ex);
    }
  }
}

package br.com.rpe.portador.application.evento;

import java.time.Instant;
import java.util.UUID;

// Envelope publicado no outbox (PRD 8.3). O relay (issue #34) usa este mesmo formato como corpo
// da mensagem SQS; correlationId/eventType/eventVersion também viram atributos SQS na publicação.
public record EventoOutbox(
    UUID eventId,
    String eventType,
    int eventVersion,
    Instant occurredAt,
    String correlationId,
    Object data) {

  public static final int VERSAO_ATUAL = 1;

  public static EventoOutbox criar(
      String eventType, String correlationId, Object data, Instant agora) {
    return new EventoOutbox(UUID.randomUUID(), eventType, VERSAO_ATUAL, agora, correlationId, data);
  }
}

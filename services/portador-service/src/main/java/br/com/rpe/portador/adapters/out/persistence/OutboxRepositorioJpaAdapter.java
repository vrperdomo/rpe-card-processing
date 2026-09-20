package br.com.rpe.portador.adapters.out.persistence;

import br.com.rpe.portador.application.evento.EventoOutbox;
import br.com.rpe.portador.application.port.out.OutboxRepositorio;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class OutboxRepositorioJpaAdapter implements OutboxRepositorio {

  private final OutboxEventJpaRepository jpaRepository;
  private final ObjectMapper objectMapper;

  public OutboxRepositorioJpaAdapter(
      OutboxEventJpaRepository jpaRepository, ObjectMapper objectMapper) {
    this.jpaRepository = jpaRepository;
    this.objectMapper = objectMapper;
  }

  @Override
  public void registrar(UUID aggregateId, String aggregateType, EventoOutbox evento) {
    jpaRepository.save(
        new OutboxEventEntity(
            evento.eventId(),
            aggregateType,
            aggregateId,
            evento.eventType(),
            serializar(evento),
            evento.occurredAt()));
  }

  private String serializar(EventoOutbox evento) {
    try {
      return objectMapper.writeValueAsString(evento);
    } catch (JsonProcessingException ex) {
      throw new IllegalStateException("Falha ao serializar evento de outbox", ex);
    }
  }
}

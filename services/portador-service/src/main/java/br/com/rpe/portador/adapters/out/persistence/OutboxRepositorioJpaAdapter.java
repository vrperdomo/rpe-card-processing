package br.com.rpe.portador.adapters.out.persistence;

import br.com.rpe.portador.application.evento.EventoOutbox;
import br.com.rpe.portador.application.evento.EventoPendente;
import br.com.rpe.portador.application.port.out.OutboxRepositorio;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.instrument.MeterRegistry;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class OutboxRepositorioJpaAdapter implements OutboxRepositorio {

  private final OutboxEventJpaRepository jpaRepository;
  private final ObjectMapper objectMapper;
  private final MeterRegistry meterRegistry;
  private final Clock clock;

  public OutboxRepositorioJpaAdapter(
      OutboxEventJpaRepository jpaRepository,
      ObjectMapper objectMapper,
      MeterRegistry meterRegistry,
      Clock clock) {
    this.jpaRepository = jpaRepository;
    this.objectMapper = objectMapper;
    this.meterRegistry = meterRegistry;
    this.clock = clock;
    meterRegistry.gauge(
        "outbox.pendentes",
        this,
        adapter -> adapter.jpaRepository.countByStatus(StatusOutboxEvent.PENDENTE));
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

  @Override
  public List<EventoPendente> buscarLotePendente(int tamanhoLote) {
    return jpaRepository.buscarLotePendente(Instant.now(clock), tamanhoLote).stream()
        .map(
            entity ->
                new EventoPendente(
                    entity.getId(),
                    entity.getAggregateId(),
                    entity.getEventType(),
                    entity.getPayload(),
                    entity.getTentativas()))
        .toList();
  }

  @Override
  public void marcarPublicado(UUID eventId, Instant publicadoEm) {
    buscar(eventId).marcarPublicado(publicadoEm);
  }

  @Override
  public void marcarFalhaTemporaria(UUID eventId, Instant proximaTentativaEm, String erro) {
    buscar(eventId).registrarFalhaTemporaria(proximaTentativaEm, erro);
  }

  @Override
  public void marcarFalhaDefinitiva(UUID eventId, String erro) {
    buscar(eventId).marcarFalhaDefinitiva(erro);
    meterRegistry.counter("outbox.falhas").increment();
  }

  private OutboxEventEntity buscar(UUID eventId) {
    return jpaRepository
        .findById(eventId)
        .orElseThrow(
            () ->
                new NoSuchElementException(
                    "Evento de outbox %s não encontrado".formatted(eventId)));
  }

  private String serializar(EventoOutbox evento) {
    try {
      return objectMapper.writeValueAsString(evento);
    } catch (JsonProcessingException ex) {
      throw new IllegalStateException("Falha ao serializar evento de outbox", ex);
    }
  }
}

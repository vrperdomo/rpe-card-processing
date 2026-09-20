package br.com.rpe.portador.adapters.out.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "outbox_event")
public class OutboxEventEntity {

  @Id private UUID id;

  @Column(name = "aggregate_type", nullable = false, length = 50)
  private String aggregateType;

  @Column(name = "aggregate_id", nullable = false)
  private UUID aggregateId;

  @Column(name = "event_type", nullable = false, length = 80)
  private String eventType;

  @JdbcTypeCode(SqlTypes.JSON)
  @Column(nullable = false, columnDefinition = "jsonb")
  private String payload;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private StatusOutboxEvent status;

  @Column(nullable = false)
  private int tentativas;

  @Column(name = "proxima_tentativa_em")
  private Instant proximaTentativaEm;

  @Column(name = "ultimo_erro")
  private String ultimoErro;

  @Column(name = "criado_em", nullable = false)
  private Instant criadoEm;

  @Column(name = "publicado_em")
  private Instant publicadoEm;

  protected OutboxEventEntity() {}

  public OutboxEventEntity(
      UUID id,
      String aggregateType,
      UUID aggregateId,
      String eventType,
      String payload,
      Instant criadoEm) {
    this.id = id;
    this.aggregateType = aggregateType;
    this.aggregateId = aggregateId;
    this.eventType = eventType;
    this.payload = payload;
    this.status = StatusOutboxEvent.PENDENTE;
    this.tentativas = 0;
    this.criadoEm = criadoEm;
  }

  public UUID getId() {
    return id;
  }

  public String getAggregateType() {
    return aggregateType;
  }

  public UUID getAggregateId() {
    return aggregateId;
  }

  public String getEventType() {
    return eventType;
  }

  public String getPayload() {
    return payload;
  }

  public StatusOutboxEvent getStatus() {
    return status;
  }

  public int getTentativas() {
    return tentativas;
  }

  public Instant getProximaTentativaEm() {
    return proximaTentativaEm;
  }

  public String getUltimoErro() {
    return ultimoErro;
  }

  public Instant getCriadoEm() {
    return criadoEm;
  }

  public Instant getPublicadoEm() {
    return publicadoEm;
  }
}

package br.com.rpe.cartao.adapters.out.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "mensagem_processada")
public class MensagemProcessadaEntity {

  @Id
  @Column(name = "event_id")
  private UUID eventId;

  @Column(name = "processado_em", nullable = false)
  private Instant processadoEm;

  protected MensagemProcessadaEntity() {}

  public MensagemProcessadaEntity(UUID eventId, Instant processadoEm) {
    this.eventId = eventId;
    this.processadoEm = processadoEm;
  }

  public UUID getEventId() {
    return eventId;
  }

  public Instant getProcessadoEm() {
    return processadoEm;
  }
}

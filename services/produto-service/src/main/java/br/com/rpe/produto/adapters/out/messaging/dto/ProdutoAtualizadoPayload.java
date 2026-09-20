package br.com.rpe.produto.adapters.out.messaging.dto;

import java.time.Instant;
import java.util.UUID;

// Envelope publicado em produto-eventos-queue (PRD 8.3). Nao carrega dados sensiveis.
public record ProdutoAtualizadoPayload(
    UUID eventId,
    String eventType,
    int eventVersion,
    Instant occurredAt,
    String correlationId,
    Dados data) {

  public static final String EVENT_TYPE = "ProdutoAtualizado";
  public static final int EVENT_VERSION = 1;

  public record Dados(UUID produtoId) {}
}

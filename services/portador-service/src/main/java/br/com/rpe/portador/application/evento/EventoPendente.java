package br.com.rpe.portador.application.evento;

import java.util.UUID;

// Representa uma linha do outbox pronta para publicacao. payload ja e o JSON serializado
// (EventoOutbox completo) gravado no cadastro — o relay nao precisa desserializar/reconstruir.
public record EventoPendente(
    UUID id, UUID aggregateId, String eventType, String payload, int tentativas) {}

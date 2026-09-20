package br.com.rpe.produto.application.evento;

import java.util.UUID;

// Evento local (Spring ApplicationEvent), nao a mensagem SQS em si: publicado dentro da transacao
// e so vira mensagem de fato apos o commit (ProdutoAtualizadoPublisher, decisao 19.3 do PRD).
public record ProdutoAtualizadoEvento(UUID produtoId, String correlationId) {}

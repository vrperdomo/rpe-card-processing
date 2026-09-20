package br.com.rpe.cartao.adapters.in.web.dto;

import br.com.rpe.cartao.domain.StatusCartao;
import java.time.Instant;
import java.util.UUID;

// pan sempre mascarado na resposta (PRD 8.4, CLAUDE.md secao 6.7) — nunca em claro pela API.
// produto vem nulo quando o Produto Service nao pode ser consultado com sucesso mas ainda assim
// resolve algo utilizavel (ex.: produto foi removido do catalogo); indisponibilidade da
// dependencia (circuito aberto/timeout) e sinalizada como 503, nao como produto nulo.
public record CartaoResponse(
    UUID id,
    UUID portadorId,
    UUID produtoId,
    String panMascarado,
    String nomeImpresso,
    String validade,
    StatusCartao status,
    ProdutoResumoResponse produto,
    Instant criadoEm,
    Instant atualizadoEm) {}

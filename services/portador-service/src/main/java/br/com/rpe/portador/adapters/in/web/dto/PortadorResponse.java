package br.com.rpe.portador.adapters.in.web.dto;

import br.com.rpe.portador.domain.StatusPortador;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

// cpf sempre mascarado na resposta (PRD 8.4, CLAUDE.md secao 6.7) — nunca em claro pela API.
public record PortadorResponse(
    UUID id,
    String nome,
    String cpf,
    LocalDate dataNascimento,
    UUID produtoId,
    StatusPortador status,
    Instant criadoEm,
    Instant atualizadoEm) {}

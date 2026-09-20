package br.com.rpe.portador.application.evento;

import java.util.UUID;

// Espelha PRD 8.3 (data). Nunca carrega CPF (minimização de dados — LGPD).
public record CartaoEmissaoSolicitadaData(UUID portadorId, UUID produtoId, String nomeImpresso) {}

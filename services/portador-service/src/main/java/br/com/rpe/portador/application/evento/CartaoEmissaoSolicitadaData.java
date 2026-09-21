package br.com.rpe.portador.application.evento;

import java.util.UUID;

// Espelha PRD 8.3 (data). Nunca carrega CPF (minimização de dados — LGPD). `criadoPor` é o `sub` do
// usuário que cadastrou o portador: o Cartão o grava como dono do cartão (ADR-009, A01).
public record CartaoEmissaoSolicitadaData(
    UUID portadorId, UUID produtoId, String nomeImpresso, String criadoPor) {}

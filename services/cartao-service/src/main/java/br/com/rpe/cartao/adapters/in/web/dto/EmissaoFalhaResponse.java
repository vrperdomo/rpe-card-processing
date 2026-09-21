package br.com.rpe.cartao.adapters.in.web.dto;

import java.time.Instant;
import java.util.UUID;

public record EmissaoFalhaResponse(
    UUID portadorId, UUID produtoId, String motivo, Instant ocorridaEm) {}

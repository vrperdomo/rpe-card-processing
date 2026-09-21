package br.com.rpe.portador.adapters.in.web.dto;

import java.time.Instant;

public record FalhaEmissaoResponse(String motivo, Instant ocorridaEm) {}

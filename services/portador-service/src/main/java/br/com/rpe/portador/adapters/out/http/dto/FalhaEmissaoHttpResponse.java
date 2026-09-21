package br.com.rpe.portador.adapters.out.http.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.time.Instant;

@JsonIgnoreProperties(ignoreUnknown = true)
public record FalhaEmissaoHttpResponse(String motivo, Instant ocorridaEm) {}

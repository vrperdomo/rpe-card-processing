package br.com.rpe.portador.adapters.out.http.dto;

import br.com.rpe.portador.application.port.out.StatusCartaoExterno;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.UUID;

@JsonIgnoreProperties(ignoreUnknown = true)
public record CartaoHttpResponse(
    UUID id, String panMascarado, String validade, StatusCartaoExterno status) {}

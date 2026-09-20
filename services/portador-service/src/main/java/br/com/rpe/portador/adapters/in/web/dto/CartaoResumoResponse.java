package br.com.rpe.portador.adapters.in.web.dto;

import br.com.rpe.portador.application.port.out.StatusCartaoExterno;
import java.util.UUID;

public record CartaoResumoResponse(
    UUID id, String panMascarado, String validade, StatusCartaoExterno status) {}

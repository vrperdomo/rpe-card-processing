package br.com.rpe.portador.adapters.in.web.dto;

import br.com.rpe.portador.domain.StatusPortador;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

public record AlterarStatusPortadorRequest(
    @Schema(example = "BLOQUEADO") @NotNull StatusPortador status) {}

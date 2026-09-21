package br.com.rpe.cartao.adapters.in.web.dto;

import br.com.rpe.cartao.domain.StatusCartao;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

public record AlterarStatusCartaoRequest(
    @Schema(example = "BLOQUEADO") @NotNull StatusCartao status) {}

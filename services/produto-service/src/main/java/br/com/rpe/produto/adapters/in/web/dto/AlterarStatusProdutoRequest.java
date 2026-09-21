package br.com.rpe.produto.adapters.in.web.dto;

import br.com.rpe.produto.domain.StatusProduto;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

public record AlterarStatusProdutoRequest(
    @Schema(example = "CANCELADO") @NotNull StatusProduto status) {}

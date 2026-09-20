package br.com.rpe.produto.adapters.in.web.dto;

import br.com.rpe.produto.domain.StatusProduto;
import jakarta.validation.constraints.NotNull;

public record AlterarStatusProdutoRequest(@NotNull StatusProduto status) {}

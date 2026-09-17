package br.com.rpe.produto.adapters.in.web.dto;

import br.com.rpe.produto.domain.CategoriaProduto;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CriarProdutoRequest(
    @NotBlank @Size(max = 100) String nome,
    @Size(max = 255) String descricao,
    @NotNull CategoriaProduto categoria,
    @NotBlank
        @Pattern(regexp = "\\d{6}", message = "bin deve conter exatamente 6 dígitos numéricos")
        String bin) {}

package br.com.rpe.produto.adapters.in.web.dto;

import br.com.rpe.produto.domain.CategoriaProduto;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CriarProdutoRequest(
    @Schema(example = "Gold Card", description = "Nome único do produto") @NotBlank @Size(max = 100)
        String nome,
    @Schema(example = "Cartão Gold com anuidade reduzida") @Size(max = 255) String descricao,
    @Schema(example = "GOLD") @NotNull CategoriaProduto categoria,
    @Schema(example = "453201", description = "Os 6 primeiros dígitos do PAN dos cartões emitidos")
        @NotBlank
        @Pattern(regexp = "\\d{6}", message = "bin deve conter exatamente 6 dígitos numéricos")
        String bin) {}

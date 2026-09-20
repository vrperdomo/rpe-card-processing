package br.com.rpe.produto.adapters.in.web.dto;

import br.com.rpe.produto.domain.CategoriaProduto;
import br.com.rpe.produto.domain.StatusProduto;
import java.time.Instant;
import java.util.UUID;

public record ProdutoResponse(
    UUID id,
    String nome,
    String descricao,
    CategoriaProduto categoria,
    String bin,
    StatusProduto status,
    Instant criadoEm,
    Instant atualizadoEm) {}

package br.com.rpe.cartao.adapters.in.web.dto;

import br.com.rpe.cartao.application.port.out.StatusProdutoExterno;
import java.util.UUID;

public record ProdutoResumoResponse(
    UUID id, String nome, String categoria, StatusProdutoExterno status) {}

package br.com.rpe.cartao.adapters.out.http.dto;

import br.com.rpe.cartao.application.port.out.StatusProdutoExterno;
import java.util.UUID;

public record ProdutoHttpResponse(
    UUID id, String nome, String categoria, String bin, StatusProdutoExterno status) {}

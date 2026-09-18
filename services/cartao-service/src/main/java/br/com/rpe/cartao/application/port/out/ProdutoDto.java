package br.com.rpe.cartao.application.port.out;

import java.util.UUID;

// Contrato próprio do Cartão, duplicado de propósito em vez de reusar o DTO do Produto Service.
public record ProdutoDto(
    UUID id, String nome, String categoria, String bin, StatusProdutoExterno status) {}

package br.com.rpe.portador.adapters.out.http.dto;

import br.com.rpe.portador.application.port.out.StatusProdutoExterno;
import java.util.UUID;

public record ProdutoHttpResponse(
    UUID id, String nome, String categoria, StatusProdutoExterno status) {}

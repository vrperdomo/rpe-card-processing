package br.com.rpe.portador.adapters.in.web.dto;

import br.com.rpe.portador.application.port.out.StatusProdutoExterno;
import java.util.UUID;

public record ProdutoResumoResponse(
    UUID id, String nome, String categoria, StatusProdutoExterno status) {}

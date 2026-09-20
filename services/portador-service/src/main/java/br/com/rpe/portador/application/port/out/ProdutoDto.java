package br.com.rpe.portador.application.port.out;

import java.util.UUID;

// Contrato próprio do Portador, duplicado de propósito em vez de reusar o DTO do Produto Service.
// nome/categoria só sao usados pela consulta agregada (PO-09); a validacao fail-fast (PO-05)
// usa so id/status.
public record ProdutoDto(UUID id, String nome, String categoria, StatusProdutoExterno status) {}

package br.com.rpe.portador.application.port.out;

import java.util.UUID;

// Contrato próprio do Portador, duplicado de propósito em vez de reusar o DTO do Produto Service.
// Só o necessário para a validação fail-fast (PO-05): existência e status.
public record ProdutoDto(UUID id, StatusProdutoExterno status) {}

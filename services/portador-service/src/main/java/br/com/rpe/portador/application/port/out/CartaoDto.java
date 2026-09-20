package br.com.rpe.portador.application.port.out;

import java.util.UUID;

// Contrato próprio do Portador, duplicado de propósito em vez de reusar o DTO do Cartão Service.
// Só o necessário para a consulta agregada (PO-09): PAN já vem mascarado da origem.
public record CartaoDto(
    UUID id, String panMascarado, String validade, StatusCartaoExterno status) {}

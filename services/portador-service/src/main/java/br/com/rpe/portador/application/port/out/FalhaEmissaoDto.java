package br.com.rpe.portador.application.port.out;

import java.time.Instant;

// Contrato próprio do Portador (issue #117): só o que a consulta agregada mostra. O motivo já vem
// do Cartão como texto seguro para o cliente.
public record FalhaEmissaoDto(String motivo, Instant ocorridaEm) {}

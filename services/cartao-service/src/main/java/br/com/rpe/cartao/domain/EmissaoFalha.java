package br.com.rpe.cartao.domain;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Registro de que a emissão do cartão de um portador falhou de vez (issue #117). {@code motivo} é
 * texto seguro para exibir ao cliente: nunca carrega dado sensível nem detalhe interno.
 */
public record EmissaoFalha(
    UUID portadorId, UUID produtoId, String motivo, String criadoPor, Instant ocorridaEm) {

  public static final int MOTIVO_TAMANHO_MAXIMO = 255;

  public EmissaoFalha {
    Objects.requireNonNull(portadorId, "portadorId não pode ser nulo");
    Objects.requireNonNull(produtoId, "produtoId não pode ser nulo");
    Objects.requireNonNull(ocorridaEm, "ocorridaEm não pode ser nulo");
    if (motivo == null || motivo.isBlank()) {
      throw new IllegalArgumentException("motivo não pode ser vazio");
    }
    if (criadoPor == null || criadoPor.isBlank()) {
      throw new IllegalArgumentException("criadoPor não pode ser vazio");
    }
    motivo =
        motivo.length() > MOTIVO_TAMANHO_MAXIMO
            ? motivo.substring(0, MOTIVO_TAMANHO_MAXIMO)
            : motivo;
  }
}

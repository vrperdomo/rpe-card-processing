package br.com.rpe.cartao.domain;

import br.com.rpe.cartao.domain.exception.RegraNegocioException;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public class Cartao {

  private static final int NOME_IMPRESSO_TAMANHO_MAXIMO = 26;

  private final UUID id;
  private final UUID portadorId;
  private final UUID produtoId;
  private final Pan pan;
  private final String nomeImpresso;
  private final Validade validade;
  private final String criadoPor;
  private StatusCartao status;
  private final Instant criadoEm;
  private Instant atualizadoEm;

  private Cartao(
      UUID id,
      UUID portadorId,
      UUID produtoId,
      Pan pan,
      String nomeImpresso,
      Validade validade,
      String criadoPor,
      StatusCartao status,
      Instant criadoEm,
      Instant atualizadoEm) {
    this.id = Objects.requireNonNull(id, "id não pode ser nulo");
    this.portadorId = Objects.requireNonNull(portadorId, "portadorId não pode ser nulo");
    this.produtoId = Objects.requireNonNull(produtoId, "produtoId não pode ser nulo");
    this.pan = Objects.requireNonNull(pan, "pan não pode ser nulo");
    this.nomeImpresso = validarNomeImpresso(nomeImpresso);
    this.validade = Objects.requireNonNull(validade, "validade não pode ser nula");
    this.criadoPor = validarCriadoPor(criadoPor);
    this.status = Objects.requireNonNull(status, "status não pode ser nulo");
    this.criadoEm = Objects.requireNonNull(criadoEm, "criadoEm não pode ser nulo");
    this.atualizadoEm = Objects.requireNonNull(atualizadoEm, "atualizadoEm não pode ser nulo");
  }

  public static Cartao emitir(
      UUID portadorId,
      UUID produtoId,
      Pan pan,
      String nomeImpresso,
      Validade validade,
      String criadoPor,
      Instant agora) {
    return new Cartao(
        UUID.randomUUID(),
        portadorId,
        produtoId,
        pan,
        nomeImpresso,
        validade,
        criadoPor,
        StatusCartao.ATIVO,
        agora,
        agora);
  }

  public static Cartao reconstituir(
      UUID id,
      UUID portadorId,
      UUID produtoId,
      Pan pan,
      String nomeImpresso,
      Validade validade,
      String criadoPor,
      StatusCartao status,
      Instant criadoEm,
      Instant atualizadoEm) {
    return new Cartao(
        id,
        portadorId,
        produtoId,
        pan,
        nomeImpresso,
        validade,
        criadoPor,
        status,
        criadoEm,
        atualizadoEm);
  }

  public void bloquear(Instant agora) {
    Objects.requireNonNull(agora, "agora não pode ser nulo");
    if (status == StatusCartao.CANCELADO) {
      throw new RegraNegocioException(
          "Cartão %s está cancelado e não pode ser bloqueado".formatted(id));
    }
    if (status == StatusCartao.BLOQUEADO) {
      throw new RegraNegocioException("Cartão %s já está bloqueado".formatted(id));
    }
    this.status = StatusCartao.BLOQUEADO;
    this.atualizadoEm = agora;
  }

  public void ativar(Instant agora) {
    Objects.requireNonNull(agora, "agora não pode ser nulo");
    if (status == StatusCartao.CANCELADO) {
      throw new RegraNegocioException(
          "Cartão %s está cancelado e não pode ser reativado".formatted(id));
    }
    if (status == StatusCartao.ATIVO) {
      throw new RegraNegocioException("Cartão %s já está ativo".formatted(id));
    }
    this.status = StatusCartao.ATIVO;
    this.atualizadoEm = agora;
  }

  public void cancelar(Instant agora) {
    Objects.requireNonNull(agora, "agora não pode ser nulo");
    if (status == StatusCartao.CANCELADO) {
      throw new RegraNegocioException("Cartão %s já está cancelado".formatted(id));
    }
    this.status = StatusCartao.CANCELADO;
    this.atualizadoEm = agora;
  }

  private static String validarCriadoPor(String criadoPor) {
    if (criadoPor == null || criadoPor.isBlank()) {
      throw new IllegalArgumentException("criadoPor não pode ser vazio");
    }
    return criadoPor;
  }

  private static String validarNomeImpresso(String nomeImpresso) {
    if (nomeImpresso == null || nomeImpresso.isBlank()) {
      throw new IllegalArgumentException("nomeImpresso não pode ser vazio");
    }
    if (nomeImpresso.length() > NOME_IMPRESSO_TAMANHO_MAXIMO) {
      throw new IllegalArgumentException(
          "nomeImpresso não pode ter mais que %d caracteres"
              .formatted(NOME_IMPRESSO_TAMANHO_MAXIMO));
    }
    return nomeImpresso;
  }

  public UUID getId() {
    return id;
  }

  public UUID getPortadorId() {
    return portadorId;
  }

  public UUID getProdutoId() {
    return produtoId;
  }

  public Pan getPan() {
    return pan;
  }

  public String getNomeImpresso() {
    return nomeImpresso;
  }

  public Validade getValidade() {
    return validade;
  }

  public String getCriadoPor() {
    return criadoPor;
  }

  public StatusCartao getStatus() {
    return status;
  }

  public Instant getCriadoEm() {
    return criadoEm;
  }

  public Instant getAtualizadoEm() {
    return atualizadoEm;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof Cartao other)) {
      return false;
    }
    return id.equals(other.id);
  }

  @Override
  public int hashCode() {
    return id.hashCode();
  }
}

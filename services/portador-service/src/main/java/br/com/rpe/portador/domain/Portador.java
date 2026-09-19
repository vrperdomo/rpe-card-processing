package br.com.rpe.portador.domain;

import br.com.rpe.portador.domain.exception.RegraNegocioException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.Period;
import java.time.ZoneOffset;
import java.util.Objects;
import java.util.UUID;

public class Portador {

  private static final int NOME_TAMANHO_MAXIMO = 150;
  private static final int IDADE_MINIMA_ANOS = 18;

  private final UUID id;
  private String nome;
  private final Cpf cpf;
  private final LocalDate dataNascimento;
  private final UUID produtoId;
  private StatusPortador status;
  private final Instant criadoEm;
  private Instant atualizadoEm;

  private Portador(
      UUID id,
      String nome,
      Cpf cpf,
      LocalDate dataNascimento,
      UUID produtoId,
      StatusPortador status,
      Instant criadoEm,
      Instant atualizadoEm) {
    this.id = Objects.requireNonNull(id, "id não pode ser nulo");
    this.nome = validarNome(nome);
    this.cpf = Objects.requireNonNull(cpf, "cpf não pode ser nulo");
    this.dataNascimento =
        Objects.requireNonNull(dataNascimento, "dataNascimento não pode ser nula");
    this.produtoId = Objects.requireNonNull(produtoId, "produtoId não pode ser nulo");
    this.status = Objects.requireNonNull(status, "status não pode ser nulo");
    this.criadoEm = Objects.requireNonNull(criadoEm, "criadoEm não pode ser nulo");
    this.atualizadoEm = Objects.requireNonNull(atualizadoEm, "atualizadoEm não pode ser nulo");
  }

  public static Portador cadastrar(
      String nome, Cpf cpf, LocalDate dataNascimento, UUID produtoId, Instant agora) {
    Objects.requireNonNull(agora, "agora não pode ser nulo");
    validarMaioridade(dataNascimento, agora);
    return new Portador(
        UUID.randomUUID(),
        nome,
        cpf,
        dataNascimento,
        produtoId,
        StatusPortador.ATIVO,
        agora,
        agora);
  }

  public static Portador reconstituir(
      UUID id,
      String nome,
      Cpf cpf,
      LocalDate dataNascimento,
      UUID produtoId,
      StatusPortador status,
      Instant criadoEm,
      Instant atualizadoEm) {
    return new Portador(id, nome, cpf, dataNascimento, produtoId, status, criadoEm, atualizadoEm);
  }

  public void bloquear(Instant agora) {
    Objects.requireNonNull(agora, "agora não pode ser nulo");
    if (status == StatusPortador.CANCELADO) {
      throw new RegraNegocioException(
          "Portador %s está cancelado e não pode ser bloqueado".formatted(id));
    }
    if (status == StatusPortador.BLOQUEADO) {
      throw new RegraNegocioException("Portador %s já está bloqueado".formatted(id));
    }
    this.status = StatusPortador.BLOQUEADO;
    this.atualizadoEm = agora;
  }

  public void ativar(Instant agora) {
    Objects.requireNonNull(agora, "agora não pode ser nulo");
    if (status == StatusPortador.CANCELADO) {
      throw new RegraNegocioException(
          "Portador %s está cancelado e não pode ser reativado".formatted(id));
    }
    if (status == StatusPortador.ATIVO) {
      throw new RegraNegocioException("Portador %s já está ativo".formatted(id));
    }
    this.status = StatusPortador.ATIVO;
    this.atualizadoEm = agora;
  }

  public void cancelar(Instant agora) {
    Objects.requireNonNull(agora, "agora não pode ser nulo");
    if (status == StatusPortador.CANCELADO) {
      throw new RegraNegocioException("Portador %s já está cancelado".formatted(id));
    }
    this.status = StatusPortador.CANCELADO;
    this.atualizadoEm = agora;
  }

  private static String validarNome(String nome) {
    if (nome == null || nome.isBlank()) {
      throw new IllegalArgumentException("nome não pode ser vazio");
    }
    if (nome.length() > NOME_TAMANHO_MAXIMO) {
      throw new IllegalArgumentException(
          "nome não pode ter mais que %d caracteres".formatted(NOME_TAMANHO_MAXIMO));
    }
    return nome;
  }

  private static void validarMaioridade(LocalDate dataNascimento, Instant agora) {
    Objects.requireNonNull(dataNascimento, "dataNascimento não pode ser nula");
    LocalDate hoje = LocalDate.ofInstant(agora, ZoneOffset.UTC);
    int idade = Period.between(dataNascimento, hoje).getYears();
    if (idade < IDADE_MINIMA_ANOS) {
      throw new RegraNegocioException(
          "Portador deve ter pelo menos %d anos completos".formatted(IDADE_MINIMA_ANOS));
    }
  }

  public UUID getId() {
    return id;
  }

  public String getNome() {
    return nome;
  }

  public Cpf getCpf() {
    return cpf;
  }

  public LocalDate getDataNascimento() {
    return dataNascimento;
  }

  public UUID getProdutoId() {
    return produtoId;
  }

  public StatusPortador getStatus() {
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
    if (!(o instanceof Portador other)) {
      return false;
    }
    return id.equals(other.id);
  }

  @Override
  public int hashCode() {
    return id.hashCode();
  }
}

package br.com.rpe.produto.domain;

import br.com.rpe.produto.domain.exception.RegraNegocioException;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;
import java.util.regex.Pattern;

public class Produto {

  private static final Pattern BIN_VALIDO = Pattern.compile("\\d{6}");
  private static final int NOME_TAMANHO_MAXIMO = 100;
  private static final int DESCRICAO_TAMANHO_MAXIMO = 255;

  private final UUID id;
  private String nome;
  private String descricao;
  private CategoriaProduto categoria;
  private String bin;
  private StatusProduto status;
  private final Instant criadoEm;
  private Instant atualizadoEm;

  private Produto(
      UUID id,
      String nome,
      String descricao,
      CategoriaProduto categoria,
      String bin,
      StatusProduto status,
      Instant criadoEm,
      Instant atualizadoEm) {
    this.id = Objects.requireNonNull(id, "id não pode ser nulo");
    this.nome = validarNome(nome);
    this.descricao = validarDescricao(descricao);
    this.categoria = validarCategoria(categoria);
    this.bin = validarBin(bin);
    this.status = Objects.requireNonNull(status, "status não pode ser nulo");
    this.criadoEm = Objects.requireNonNull(criadoEm, "criadoEm não pode ser nulo");
    this.atualizadoEm = Objects.requireNonNull(atualizadoEm, "atualizadoEm não pode ser nulo");
  }

  public static Produto criar(
      String nome, String descricao, CategoriaProduto categoria, String bin, Instant agora) {
    return new Produto(
        UUID.randomUUID(), nome, descricao, categoria, bin, StatusProduto.ATIVO, agora, agora);
  }

  public static Produto reconstituir(
      UUID id,
      String nome,
      String descricao,
      CategoriaProduto categoria,
      String bin,
      StatusProduto status,
      Instant criadoEm,
      Instant atualizadoEm) {
    return new Produto(id, nome, descricao, categoria, bin, status, criadoEm, atualizadoEm);
  }

  public void cancelar(Instant agora) {
    Objects.requireNonNull(agora, "agora não pode ser nulo");
    if (status == StatusProduto.CANCELADO) {
      throw new RegraNegocioException("Produto %s já está cancelado".formatted(id));
    }
    this.status = StatusProduto.CANCELADO;
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

  private static String validarDescricao(String descricao) {
    if (descricao != null && descricao.length() > DESCRICAO_TAMANHO_MAXIMO) {
      throw new IllegalArgumentException(
          "descrição não pode ter mais que %d caracteres".formatted(DESCRICAO_TAMANHO_MAXIMO));
    }
    return descricao;
  }

  private static CategoriaProduto validarCategoria(CategoriaProduto categoria) {
    if (categoria == null) {
      throw new IllegalArgumentException("categoria não pode ser nula");
    }
    return categoria;
  }

  private static String validarBin(String bin) {
    if (bin == null || !BIN_VALIDO.matcher(bin).matches()) {
      throw new IllegalArgumentException("bin deve conter exatamente 6 dígitos numéricos");
    }
    return bin;
  }

  public UUID getId() {
    return id;
  }

  public String getNome() {
    return nome;
  }

  public String getDescricao() {
    return descricao;
  }

  public CategoriaProduto getCategoria() {
    return categoria;
  }

  public String getBin() {
    return bin;
  }

  public StatusProduto getStatus() {
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
    if (!(o instanceof Produto other)) {
      return false;
    }
    return id.equals(other.id);
  }

  @Override
  public int hashCode() {
    return id.hashCode();
  }
}

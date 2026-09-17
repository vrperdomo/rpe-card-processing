package br.com.rpe.produto.adapters.out.persistence;

import br.com.rpe.produto.domain.CategoriaProduto;
import br.com.rpe.produto.domain.StatusProduto;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Instant;
import java.util.UUID;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

@Entity
@Table(name = "produto")
@EntityListeners(AuditingEntityListener.class)
public class ProdutoEntity {

  @Id private UUID id;

  @Column(nullable = false, unique = true, length = 100)
  private String nome;

  @Column(length = 255)
  private String descricao;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 30)
  private CategoriaProduto categoria;

  @Column(nullable = false, length = 6)
  private String bin;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private StatusProduto status;

  @Version
  @Column(nullable = false)
  private Long versao;

  @CreatedDate
  @Column(name = "criado_em", nullable = false, updatable = false)
  private Instant criadoEm;

  @LastModifiedDate
  @Column(name = "atualizado_em", nullable = false)
  private Instant atualizadoEm;

  protected ProdutoEntity() {}

  public ProdutoEntity(
      UUID id,
      String nome,
      String descricao,
      CategoriaProduto categoria,
      String bin,
      StatusProduto status) {
    this.id = id;
    this.nome = nome;
    this.descricao = descricao;
    this.categoria = categoria;
    this.bin = bin;
    this.status = status;
  }

  public UUID getId() {
    return id;
  }

  public String getNome() {
    return nome;
  }

  public void setNome(String nome) {
    this.nome = nome;
  }

  public String getDescricao() {
    return descricao;
  }

  public void setDescricao(String descricao) {
    this.descricao = descricao;
  }

  public CategoriaProduto getCategoria() {
    return categoria;
  }

  public void setCategoria(CategoriaProduto categoria) {
    this.categoria = categoria;
  }

  public String getBin() {
    return bin;
  }

  public void setBin(String bin) {
    this.bin = bin;
  }

  public StatusProduto getStatus() {
    return status;
  }

  public void setStatus(StatusProduto status) {
    this.status = status;
  }

  public Long getVersao() {
    return versao;
  }

  public Instant getCriadoEm() {
    return criadoEm;
  }

  public Instant getAtualizadoEm() {
    return atualizadoEm;
  }
}

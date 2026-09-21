package br.com.rpe.cartao.adapters.out.persistence;

import br.com.rpe.cartao.domain.StatusCartao;
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
@Table(name = "cartao")
@EntityListeners(AuditingEntityListener.class)
public class CartaoEntity {

  @Id private UUID id;

  @Column(name = "portador_id", nullable = false)
  private UUID portadorId;

  @Column(name = "produto_id", nullable = false)
  private UUID produtoId;

  @Column(name = "pan_cifrado", nullable = false, length = 255)
  private String panCifrado;

  @Column(name = "pan_hash", nullable = false, unique = true, length = 64)
  private String panHash;

  @Column(nullable = false, length = 4)
  private String ultimos4;

  @Column(name = "nome_impresso", nullable = false, length = 26)
  private String nomeImpresso;

  @Column(nullable = false, length = 5)
  private String validade;

  @Column(name = "criado_por", nullable = false, updatable = false)
  private String criadoPor;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private StatusCartao status;

  @Version
  @Column(nullable = false)
  private Long versao;

  @CreatedDate
  @Column(name = "criado_em", nullable = false, updatable = false)
  private Instant criadoEm;

  @LastModifiedDate
  @Column(name = "atualizado_em", nullable = false)
  private Instant atualizadoEm;

  protected CartaoEntity() {}

  public CartaoEntity(
      UUID id,
      UUID portadorId,
      UUID produtoId,
      String panCifrado,
      String panHash,
      String ultimos4,
      String nomeImpresso,
      String validade,
      String criadoPor,
      StatusCartao status) {
    this.id = id;
    this.portadorId = portadorId;
    this.produtoId = produtoId;
    this.panCifrado = panCifrado;
    this.panHash = panHash;
    this.ultimos4 = ultimos4;
    this.nomeImpresso = nomeImpresso;
    this.validade = validade;
    this.criadoPor = criadoPor;
    this.status = status;
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

  public String getCriadoPor() {
    return criadoPor;
  }

  public String getPanCifrado() {
    return panCifrado;
  }

  public String getPanHash() {
    return panHash;
  }

  public String getUltimos4() {
    return ultimos4;
  }

  public String getNomeImpresso() {
    return nomeImpresso;
  }

  public String getValidade() {
    return validade;
  }

  public StatusCartao getStatus() {
    return status;
  }

  public void setStatus(StatusCartao status) {
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

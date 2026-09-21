package br.com.rpe.cartao.adapters.out.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "emissao_falha")
public class EmissaoFalhaEntity {

  @Id
  @Column(name = "portador_id")
  private UUID portadorId;

  @Column(name = "produto_id", nullable = false)
  private UUID produtoId;

  @Column(nullable = false)
  private String motivo;

  @Column(name = "criado_por", nullable = false)
  private String criadoPor;

  @Column(name = "ocorrida_em", nullable = false)
  private Instant ocorridaEm;

  protected EmissaoFalhaEntity() {}

  public EmissaoFalhaEntity(
      UUID portadorId, UUID produtoId, String motivo, String criadoPor, Instant ocorridaEm) {
    this.portadorId = portadorId;
    this.produtoId = produtoId;
    this.motivo = motivo;
    this.criadoPor = criadoPor;
    this.ocorridaEm = ocorridaEm;
  }

  public UUID getPortadorId() {
    return portadorId;
  }

  public UUID getProdutoId() {
    return produtoId;
  }

  public String getMotivo() {
    return motivo;
  }

  public String getCriadoPor() {
    return criadoPor;
  }

  public Instant getOcorridaEm() {
    return ocorridaEm;
  }
}

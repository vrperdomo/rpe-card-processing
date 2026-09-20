package br.com.rpe.portador.adapters.out.persistence;

import br.com.rpe.portador.domain.StatusPortador;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

@Entity
@Table(name = "portador")
@EntityListeners(AuditingEntityListener.class)
public class PortadorEntity {

  @Id private UUID id;

  @Column(nullable = false, length = 150)
  private String nome;

  @Column(nullable = false, unique = true, length = 11)
  private String cpf;

  @Column(name = "data_nascimento", nullable = false)
  private LocalDate dataNascimento;

  @Column(name = "produto_id", nullable = false)
  private UUID produtoId;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private StatusPortador status;

  @Version
  @Column(nullable = false)
  private Long versao;

  @CreatedDate
  @Column(name = "criado_em", nullable = false, updatable = false)
  private Instant criadoEm;

  @LastModifiedDate
  @Column(name = "atualizado_em", nullable = false)
  private Instant atualizadoEm;

  protected PortadorEntity() {}

  public PortadorEntity(
      UUID id,
      String nome,
      String cpf,
      LocalDate dataNascimento,
      UUID produtoId,
      StatusPortador status) {
    this.id = id;
    this.nome = nome;
    this.cpf = cpf;
    this.dataNascimento = dataNascimento;
    this.produtoId = produtoId;
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

  public String getCpf() {
    return cpf;
  }

  public void setCpf(String cpf) {
    this.cpf = cpf;
  }

  public LocalDate getDataNascimento() {
    return dataNascimento;
  }

  public void setDataNascimento(LocalDate dataNascimento) {
    this.dataNascimento = dataNascimento;
  }

  public UUID getProdutoId() {
    return produtoId;
  }

  public void setProdutoId(UUID produtoId) {
    this.produtoId = produtoId;
  }

  public StatusPortador getStatus() {
    return status;
  }

  public void setStatus(StatusPortador status) {
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

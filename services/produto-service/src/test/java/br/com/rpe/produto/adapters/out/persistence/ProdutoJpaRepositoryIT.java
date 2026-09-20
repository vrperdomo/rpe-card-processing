package br.com.rpe.produto.adapters.out.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.com.rpe.produto.IntegrationTestBase;
import br.com.rpe.produto.config.JpaAuditingConfig;
import br.com.rpe.produto.domain.CategoriaProduto;
import br.com.rpe.produto.domain.StatusProduto;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({JpaAuditingConfig.class, ProdutoJpaRepositoryIT.RelogioDeTesteConfig.class})
class ProdutoJpaRepositoryIT extends IntegrationTestBase {

  @Autowired private ProdutoJpaRepository repository;
  @Autowired private TestEntityManager entityManager;
  @Autowired private RelogioAjustavel relogio;

  @Test
  void devePersistirEPreencherAuditoriaEVersaoAutomaticamente() {
    ProdutoEntity produto = repository.saveAndFlush(criarEntity("Gold"));

    assertThat(produto.getVersao()).isEqualTo(0L);
    assertThat(produto.getCriadoEm()).isEqualTo(relogio.instant());
    assertThat(produto.getAtualizadoEm()).isEqualTo(relogio.instant());
  }

  @Test
  void deveAtualizarAtualizadoEmSemAlterarCriadoEmAoModificar() {
    ProdutoEntity salvo = repository.saveAndFlush(criarEntity("Gold"));
    Instant criadoEmOriginal = salvo.getCriadoEm();
    entityManager.clear();

    relogio.avancar(60);
    ProdutoEntity carregado = repository.findById(salvo.getId()).orElseThrow();
    carregado.setDescricao("Descrição atualizada");
    ProdutoEntity atualizado = repository.saveAndFlush(carregado);

    assertThat(atualizado.getVersao()).isEqualTo(1L);
    assertThat(atualizado.getCriadoEm()).isEqualTo(criadoEmOriginal);
    assertThat(atualizado.getAtualizadoEm()).isEqualTo(relogio.instant());
  }

  @Test
  void naoDevePermitirNomeDuplicado() {
    repository.saveAndFlush(criarEntity("Gold"));

    assertThatThrownBy(() -> repository.saveAndFlush(criarEntity("Gold")))
        .isInstanceOf(DataIntegrityViolationException.class);
  }

  @Test
  void deveLancarExcecaoDeOptimisticLockEmEscritaConcorrente() {
    ProdutoEntity salvo = repository.saveAndFlush(criarEntity("Gold"));
    entityManager.clear();

    ProdutoEntity primeiraLeitura = repository.findById(salvo.getId()).orElseThrow();
    entityManager.clear();
    ProdutoEntity segundaLeitura = repository.findById(salvo.getId()).orElseThrow();
    entityManager.clear();

    primeiraLeitura.setDescricao("Alterado pela primeira leitura");
    repository.saveAndFlush(primeiraLeitura);

    segundaLeitura.setDescricao("Alterado pela segunda leitura");
    assertThatThrownBy(() -> repository.saveAndFlush(segundaLeitura))
        .isInstanceOf(ObjectOptimisticLockingFailureException.class);
  }

  private ProdutoEntity criarEntity(String nome) {
    return new ProdutoEntity(
        UUID.randomUUID(), nome, "descrição", CategoriaProduto.GOLD, "123456", StatusProduto.ATIVO);
  }

  @TestConfiguration
  static class RelogioDeTesteConfig {

    @Bean
    RelogioAjustavel relogioAjustavel() {
      return new RelogioAjustavel(Instant.parse("2026-09-17T12:00:00Z"));
    }
  }

  static final class RelogioAjustavel extends Clock {

    private Instant instante;

    RelogioAjustavel(Instant instanteInicial) {
      this.instante = instanteInicial;
    }

    void avancar(long segundos) {
      instante = instante.plusSeconds(segundos);
    }

    @Override
    public ZoneId getZone() {
      return ZoneOffset.UTC;
    }

    @Override
    public Clock withZone(ZoneId zone) {
      return this;
    }

    @Override
    public Instant instant() {
      return instante;
    }
  }
}

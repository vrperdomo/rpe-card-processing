package br.com.rpe.cartao.adapters.out.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.com.rpe.cartao.IntegrationTestBase;
import br.com.rpe.cartao.adapters.out.security.PanCriptografoAesGcm;
import br.com.rpe.cartao.config.ClockConfig;
import br.com.rpe.cartao.config.JpaAuditingConfig;
import br.com.rpe.cartao.config.PanCriptografiaConfig;
import br.com.rpe.cartao.domain.Cartao;
import br.com.rpe.cartao.domain.Pan;
import br.com.rpe.cartao.domain.StatusCartao;
import br.com.rpe.cartao.domain.Validade;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;
import org.hibernate.exception.ConstraintViolationException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({
  ClockConfig.class,
  JpaAuditingConfig.class,
  PanCriptografiaConfig.class,
  PanCriptografoAesGcm.class,
  CartaoEntityMapper.class,
  CartaoRepositorioJpaAdapter.class
})
class CartaoRepositorioJpaAdapterIT extends IntegrationTestBase {

  private static final Instant AGORA = Instant.parse("2026-09-19T12:00:00Z");
  private static final Clock RELOGIO = Clock.fixed(AGORA, ZoneOffset.UTC);

  @Autowired private CartaoRepositorioJpaAdapter adapter;
  @Autowired private TestEntityManager entityManager;

  private Cartao novoCartao(UUID portadorId, UUID produtoId, String pan) {
    return Cartao.emitir(
        portadorId, produtoId, Pan.of(pan), "VICTOR RODRIGUES", Validade.gerar(AGORA), AGORA);
  }

  @Test
  void deveSalvarERecuperarCartaoDecifrandoOPan() {
    UUID portadorId = UUID.randomUUID();
    UUID produtoId = UUID.randomUUID();
    Cartao cartao = novoCartao(portadorId, produtoId, "4532015112830366");

    Cartao salvo = adapter.salvar(cartao);
    entityManager.flush();
    entityManager.clear();

    Cartao recuperado = adapter.buscarPorId(salvo.getId()).orElseThrow();
    assertThat(recuperado.getPan()).isEqualTo(Pan.of("4532015112830366"));
    assertThat(recuperado.getPortadorId()).isEqualTo(portadorId);
    assertThat(recuperado.getProdutoId()).isEqualTo(produtoId);
    assertThat(recuperado.getNomeImpresso()).isEqualTo("VICTOR RODRIGUES");
    assertThat(recuperado.getStatus()).isEqualTo(StatusCartao.ATIVO);
  }

  @Test
  void deveCifrarOPanAntesDePersistir() {
    Cartao cartao = novoCartao(UUID.randomUUID(), UUID.randomUUID(), "4532015112830366");

    Cartao salvo = adapter.salvar(cartao);
    entityManager.flush();
    entityManager.clear();

    CartaoEntity entity = entityManager.find(CartaoEntity.class, salvo.getId());
    assertThat(entity.getPanCifrado()).isNotEqualTo("4532015112830366");
    assertThat(entity.getPanCifrado()).doesNotContain("4532015112830366");
    assertThat(entity.getPanHash()).hasSize(64);
  }

  @Test
  void deveAtualizarStatusDeCartaoExistenteAoSalvarNovamente() {
    Cartao cartao =
        adapter.salvar(novoCartao(UUID.randomUUID(), UUID.randomUUID(), "4532015112830366"));
    entityManager.flush();
    entityManager.clear();

    Cartao carregado = adapter.buscarPorId(cartao.getId()).orElseThrow();
    carregado.bloquear(RELOGIO.instant());
    adapter.salvar(carregado);
    entityManager.flush();
    entityManager.clear();

    Cartao recarregado = adapter.buscarPorId(cartao.getId()).orElseThrow();
    assertThat(recarregado.getStatus()).isEqualTo(StatusCartao.BLOQUEADO);
  }

  @Test
  void devePropagarExistenciaPorPortadorEProduto() {
    UUID portadorId = UUID.randomUUID();
    UUID produtoId = UUID.randomUUID();
    adapter.salvar(novoCartao(portadorId, produtoId, "4532015112830366"));

    assertThat(adapter.existePorPortadorEProduto(portadorId, produtoId)).isTrue();
    assertThat(adapter.existePorPortadorEProduto(UUID.randomUUID(), produtoId)).isFalse();
  }

  @Test
  void deveGarantirUnicidadeDePanHashNoBanco() {
    adapter.salvar(novoCartao(UUID.randomUUID(), UUID.randomUUID(), "4532015112830366"));
    entityManager.flush();

    // A violação só ocorre no flush manual (fora do proxy do Spring Data), por isso chega como
    // exceção crua do Hibernate, não traduzida para DataIntegrityViolationException.
    assertThatThrownBy(
            () -> {
              adapter.salvar(novoCartao(UUID.randomUUID(), UUID.randomUUID(), "4532015112830366"));
              entityManager.flush();
            })
        .isInstanceOf(ConstraintViolationException.class);
  }

  @Test
  void deveGarantirUnicidadeDePortadorEProdutoNoBanco() {
    UUID portadorId = UUID.randomUUID();
    UUID produtoId = UUID.randomUUID();
    adapter.salvar(novoCartao(portadorId, produtoId, "4532015112830366"));
    entityManager.flush();

    assertThatThrownBy(
            () -> {
              adapter.salvar(novoCartao(portadorId, produtoId, "4916338506082832"));
              entityManager.flush();
            })
        .isInstanceOf(ConstraintViolationException.class);
  }
}

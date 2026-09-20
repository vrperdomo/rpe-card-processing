package br.com.rpe.produto.adapters.out.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.rpe.produto.IntegrationTestBase;
import br.com.rpe.produto.config.ClockConfig;
import br.com.rpe.produto.config.JpaAuditingConfig;
import br.com.rpe.produto.domain.CategoriaProduto;
import br.com.rpe.produto.domain.Produto;
import br.com.rpe.produto.domain.StatusProduto;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageRequest;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({
  ClockConfig.class,
  JpaAuditingConfig.class,
  ProdutoEntityMapperImpl.class,
  ProdutoRepositorioJpaAdapter.class
})
class ProdutoRepositorioJpaAdapterIT extends IntegrationTestBase {

  private static final Instant AGORA = Instant.parse("2026-09-17T12:00:00Z");
  private static final Clock RELOGIO = Clock.fixed(AGORA, ZoneOffset.UTC);

  @Autowired private ProdutoRepositorioJpaAdapter adapter;
  @Autowired private TestEntityManager entityManager;

  @Test
  void deveSalvarERecuperarProdutoPreservandoDados() {
    Produto produto = Produto.criar("Gold", "descricao", CategoriaProduto.GOLD, "123456", AGORA);

    Produto salvo = adapter.salvar(produto);
    entityManager.flush();
    entityManager.clear();

    Produto recuperado = adapter.buscarPorId(salvo.getId()).orElseThrow();
    assertThat(recuperado.getNome()).isEqualTo("Gold");
    assertThat(recuperado.getDescricao()).isEqualTo("descricao");
    assertThat(recuperado.getCategoria()).isEqualTo(CategoriaProduto.GOLD);
    assertThat(recuperado.getBin()).isEqualTo("123456");
    assertThat(recuperado.getStatus()).isEqualTo(StatusProduto.ATIVO);
  }

  @Test
  void deveAtualizarProdutoExistenteAoSalvarNovamente() {
    Produto produto =
        adapter.salvar(Produto.criar("Gold", "descricao", CategoriaProduto.GOLD, "123456", AGORA));
    entityManager.flush();
    entityManager.clear();

    Produto carregado = adapter.buscarPorId(produto.getId()).orElseThrow();
    carregado.cancelar(RELOGIO.instant());
    adapter.salvar(carregado);
    entityManager.flush();
    entityManager.clear();

    Produto recarregado = adapter.buscarPorId(produto.getId()).orElseThrow();
    assertThat(recarregado.getStatus()).isEqualTo(StatusProduto.CANCELADO);
  }

  @Test
  void devePropagarExistenciaPorNome() {
    adapter.salvar(Produto.criar("Gold", "descricao", CategoriaProduto.GOLD, "123456", AGORA));

    assertThat(adapter.existePorNome("Gold")).isTrue();
    assertThat(adapter.existePorNome("Platinum")).isFalse();
  }

  @Test
  void deveListarApenasProdutosDoStatusFiltrado() {
    adapter.salvar(Produto.criar("Gold", "descricao", CategoriaProduto.GOLD, "123456", AGORA));
    Produto cancelado =
        adapter.salvar(
            Produto.criar("Platinum", "descricao", CategoriaProduto.PLATINUM, "654321", AGORA));
    entityManager.flush();
    entityManager.clear();
    Produto carregado = adapter.buscarPorId(cancelado.getId()).orElseThrow();
    carregado.cancelar(RELOGIO.instant());
    adapter.salvar(carregado);
    entityManager.flush();

    var pagina = adapter.listar(StatusProduto.CANCELADO, PageRequest.of(0, 10));

    assertThat(pagina.getContent()).extracting(Produto::getNome).containsExactly("Platinum");
  }
}

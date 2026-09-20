package br.com.rpe.produto.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.com.rpe.produto.domain.exception.RegraNegocioException;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ProdutoTest {

  private static final Instant AGORA = Instant.parse("2026-09-17T12:00:00Z");

  @Test
  void deveCriarProdutoAtivoComDadosInformados() {
    var produto =
        Produto.criar("Gold", "Cartão categoria Gold", CategoriaProduto.GOLD, "123456", AGORA);

    assertThat(produto.getId()).isNotNull();
    assertThat(produto.getNome()).isEqualTo("Gold");
    assertThat(produto.getDescricao()).isEqualTo("Cartão categoria Gold");
    assertThat(produto.getCategoria()).isEqualTo(CategoriaProduto.GOLD);
    assertThat(produto.getBin()).isEqualTo("123456");
    assertThat(produto.getStatus()).isEqualTo(StatusProduto.ATIVO);
    assertThat(produto.getCriadoEm()).isEqualTo(AGORA);
    assertThat(produto.getAtualizadoEm()).isEqualTo(AGORA);
  }

  @Test
  void deveLancarExcecaoQuandoNomeEmBranco() {
    assertThatThrownBy(
            () -> Produto.criar("  ", "descricao", CategoriaProduto.GOLD, "123456", AGORA))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void deveLancarExcecaoQuandoBinNaoTiverSeisDigitos() {
    assertThatThrownBy(
            () -> Produto.criar("Gold", "descricao", CategoriaProduto.GOLD, "12345", AGORA))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void deveLancarExcecaoQuandoBinContiverLetras() {
    assertThatThrownBy(
            () -> Produto.criar("Gold", "descricao", CategoriaProduto.GOLD, "12345A", AGORA))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void deveLancarExcecaoQuandoCategoriaForNula() {
    assertThatThrownBy(() -> Produto.criar("Gold", "descricao", null, "123456", AGORA))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void deveCancelarProdutoAtivo() {
    var produto = Produto.criar("Gold", "descricao", CategoriaProduto.GOLD, "123456", AGORA);
    var depois = AGORA.plusSeconds(60);

    produto.cancelar(depois);

    assertThat(produto.getStatus()).isEqualTo(StatusProduto.CANCELADO);
    assertThat(produto.getAtualizadoEm()).isEqualTo(depois);
  }

  @Test
  void naoDeveCancelarProdutoJaCancelado() {
    var produto = Produto.criar("Gold", "descricao", CategoriaProduto.GOLD, "123456", AGORA);
    produto.cancelar(AGORA.plusSeconds(60));

    assertThatThrownBy(() -> produto.cancelar(AGORA.plusSeconds(120)))
        .isInstanceOf(RegraNegocioException.class);
  }

  @Test
  void naoDeveCancelarComInstanteNuloNemAlterarEstado() {
    var produto = Produto.criar("Gold", "descricao", CategoriaProduto.GOLD, "123456", AGORA);

    assertThatThrownBy(() -> produto.cancelar(null)).isInstanceOf(NullPointerException.class);

    assertThat(produto.getStatus()).isEqualTo(StatusProduto.ATIVO);
    assertThat(produto.getAtualizadoEm()).isEqualTo(AGORA);
  }

  @Test
  void deveReconstituirProdutoExistentePreservandoDados() {
    var id = UUID.randomUUID();
    var criadoEm = AGORA.minusSeconds(3600);

    var produto =
        Produto.reconstituir(
            id,
            "Platinum",
            "descricao",
            CategoriaProduto.PLATINUM,
            "654321",
            StatusProduto.CANCELADO,
            criadoEm,
            AGORA);

    assertThat(produto.getId()).isEqualTo(id);
    assertThat(produto.getStatus()).isEqualTo(StatusProduto.CANCELADO);
    assertThat(produto.getCriadoEm()).isEqualTo(criadoEm);
    assertThat(produto.getAtualizadoEm()).isEqualTo(AGORA);
  }

  @Test
  void doisProdutosComMesmoIdDevemSerIguais() {
    var id = UUID.randomUUID();
    var produtoA =
        Produto.reconstituir(
            id, "Gold", "d", CategoriaProduto.GOLD, "123456", StatusProduto.ATIVO, AGORA, AGORA);
    var produtoB =
        Produto.reconstituir(
            id,
            "Outro nome",
            "d2",
            CategoriaProduto.BLACK,
            "654321",
            StatusProduto.CANCELADO,
            AGORA,
            AGORA);

    assertThat(produtoA).isEqualTo(produtoB);
    assertThat(produtoA).hasSameHashCodeAs(produtoB);
  }
}

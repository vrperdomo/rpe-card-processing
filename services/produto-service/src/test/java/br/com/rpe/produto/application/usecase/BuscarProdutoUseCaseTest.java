package br.com.rpe.produto.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import br.com.rpe.produto.application.port.out.ProdutoRepositorio;
import br.com.rpe.produto.domain.CategoriaProduto;
import br.com.rpe.produto.domain.Produto;
import br.com.rpe.produto.domain.exception.RecursoNaoEncontradoException;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class BuscarProdutoUseCaseTest {

  private static final Instant AGORA = Instant.parse("2026-09-17T12:00:00Z");

  @Mock private ProdutoRepositorio produtoRepositorio;

  private BuscarProdutoUseCase useCase;

  @BeforeEach
  void configurar() {
    useCase = new BuscarProdutoUseCase(produtoRepositorio);
  }

  @Test
  void deveRetornarProdutoQuandoExiste() {
    Produto produto = Produto.criar("Gold", "descricao", CategoriaProduto.GOLD, "123456", AGORA);
    when(produtoRepositorio.buscarPorId(produto.getId())).thenReturn(Optional.of(produto));

    Produto encontrado = useCase.executar(produto.getId());

    assertThat(encontrado).isEqualTo(produto);
  }

  @Test
  void deveLancarRecursoNaoEncontradoQuandoProdutoNaoExiste() {
    UUID id = UUID.randomUUID();
    when(produtoRepositorio.buscarPorId(id)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> useCase.executar(id))
        .isInstanceOf(RecursoNaoEncontradoException.class);
  }
}

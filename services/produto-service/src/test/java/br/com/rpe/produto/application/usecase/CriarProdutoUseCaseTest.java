package br.com.rpe.produto.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import br.com.rpe.produto.application.port.out.ProdutoRepositorio;
import br.com.rpe.produto.domain.CategoriaProduto;
import br.com.rpe.produto.domain.Produto;
import br.com.rpe.produto.domain.exception.ConflitoException;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CriarProdutoUseCaseTest {

  private static final Clock RELOGIO =
      Clock.fixed(Instant.parse("2026-09-17T12:00:00Z"), ZoneOffset.UTC);

  @Mock private ProdutoRepositorio produtoRepositorio;

  private CriarProdutoUseCase useCase;

  @BeforeEach
  void configurar() {
    useCase = new CriarProdutoUseCase(produtoRepositorio, RELOGIO);
  }

  @Test
  void deveCriarProdutoQuandoNomeNaoExiste() {
    when(produtoRepositorio.existePorNome("Gold")).thenReturn(false);
    when(produtoRepositorio.salvar(any(Produto.class)))
        .thenAnswer(chamada -> chamada.getArgument(0));

    Produto produto = useCase.executar("Gold", "descricao", CategoriaProduto.GOLD, "123456");

    assertThat(produto.getNome()).isEqualTo("Gold");
    assertThat(produto.getCriadoEm()).isEqualTo(RELOGIO.instant());
    verify(produtoRepositorio).salvar(any(Produto.class));
  }

  @Test
  void deveLancarConflitoQuandoNomeJaExiste() {
    when(produtoRepositorio.existePorNome("Gold")).thenReturn(true);

    assertThatThrownBy(() -> useCase.executar("Gold", "descricao", CategoriaProduto.GOLD, "123456"))
        .isInstanceOf(ConflitoException.class);

    verify(produtoRepositorio, never()).salvar(any());
  }
}

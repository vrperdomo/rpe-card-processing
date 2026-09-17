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
import br.com.rpe.produto.domain.StatusProduto;
import br.com.rpe.produto.domain.exception.RecursoNaoEncontradoException;
import br.com.rpe.produto.domain.exception.RegraNegocioException;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AlterarStatusProdutoUseCaseTest {

  private static final Clock RELOGIO =
      Clock.fixed(Instant.parse("2026-09-17T12:00:00Z"), ZoneOffset.UTC);

  @Mock private ProdutoRepositorio produtoRepositorio;

  private AlterarStatusProdutoUseCase useCase;

  @BeforeEach
  void configurar() {
    useCase = new AlterarStatusProdutoUseCase(produtoRepositorio, RELOGIO);
  }

  @Test
  void deveCancelarProdutoQuandoNovoStatusForCancelado() {
    Produto produto =
        Produto.criar("Gold", "descricao", CategoriaProduto.GOLD, "123456", RELOGIO.instant());
    when(produtoRepositorio.buscarPorId(produto.getId())).thenReturn(Optional.of(produto));
    when(produtoRepositorio.salvar(any(Produto.class)))
        .thenAnswer(chamada -> chamada.getArgument(0));

    Produto atualizado = useCase.executar(produto.getId(), StatusProduto.CANCELADO);

    assertThat(atualizado.getStatus()).isEqualTo(StatusProduto.CANCELADO);
  }

  @Test
  void deveLancarRegraNegocioQuandoNovoStatusNaoForCancelado() {
    UUID id = UUID.randomUUID();

    assertThatThrownBy(() -> useCase.executar(id, StatusProduto.ATIVO))
        .isInstanceOf(RegraNegocioException.class);

    verify(produtoRepositorio, never()).buscarPorId(any());
  }

  @Test
  void deveLancarRecursoNaoEncontradoQuandoProdutoNaoExiste() {
    UUID id = UUID.randomUUID();
    when(produtoRepositorio.buscarPorId(id)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> useCase.executar(id, StatusProduto.CANCELADO))
        .isInstanceOf(RecursoNaoEncontradoException.class);
  }
}

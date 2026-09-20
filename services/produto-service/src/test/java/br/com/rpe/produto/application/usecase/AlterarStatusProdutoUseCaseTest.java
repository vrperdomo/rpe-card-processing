package br.com.rpe.produto.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import br.com.rpe.produto.application.evento.ProdutoAtualizadoEvento;
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
import org.springframework.context.ApplicationEventPublisher;

@ExtendWith(MockitoExtension.class)
class AlterarStatusProdutoUseCaseTest {

  private static final Clock RELOGIO =
      Clock.fixed(Instant.parse("2026-09-17T12:00:00Z"), ZoneOffset.UTC);

  @Mock private ProdutoRepositorio produtoRepositorio;
  @Mock private ApplicationEventPublisher eventPublisher;

  private AlterarStatusProdutoUseCase useCase;

  @BeforeEach
  void configurar() {
    useCase = new AlterarStatusProdutoUseCase(produtoRepositorio, eventPublisher, RELOGIO);
  }

  @Test
  void deveCancelarProdutoQuandoNovoStatusForCancelado() {
    Produto produto =
        Produto.criar("Gold", "descricao", CategoriaProduto.GOLD, "123456", RELOGIO.instant());
    when(produtoRepositorio.buscarPorId(produto.getId())).thenReturn(Optional.of(produto));
    when(produtoRepositorio.salvar(any(Produto.class)))
        .thenAnswer(chamada -> chamada.getArgument(0));

    Produto atualizado = useCase.executar(produto.getId(), StatusProduto.CANCELADO, "corr-1");

    assertThat(atualizado.getStatus()).isEqualTo(StatusProduto.CANCELADO);
  }

  @Test
  void devePublicarEventoProdutoAtualizadoAoCancelar() {
    Produto produto =
        Produto.criar("Gold", "descricao", CategoriaProduto.GOLD, "123456", RELOGIO.instant());
    when(produtoRepositorio.buscarPorId(produto.getId())).thenReturn(Optional.of(produto));
    when(produtoRepositorio.salvar(any(Produto.class)))
        .thenAnswer(chamada -> chamada.getArgument(0));

    useCase.executar(produto.getId(), StatusProduto.CANCELADO, "corr-1");

    verify(eventPublisher).publishEvent(new ProdutoAtualizadoEvento(produto.getId(), "corr-1"));
  }

  @Test
  void deveLancarRegraNegocioQuandoNovoStatusNaoForCancelado() {
    UUID id = UUID.randomUUID();

    assertThatThrownBy(() -> useCase.executar(id, StatusProduto.ATIVO, "corr-1"))
        .isInstanceOf(RegraNegocioException.class);

    verify(produtoRepositorio, never()).buscarPorId(any());
    verify(eventPublisher, never()).publishEvent(any());
  }

  @Test
  void deveLancarRecursoNaoEncontradoQuandoProdutoNaoExiste() {
    UUID id = UUID.randomUUID();
    when(produtoRepositorio.buscarPorId(id)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> useCase.executar(id, StatusProduto.CANCELADO, "corr-1"))
        .isInstanceOf(RecursoNaoEncontradoException.class);

    verify(eventPublisher, never()).publishEvent(any());
  }
}

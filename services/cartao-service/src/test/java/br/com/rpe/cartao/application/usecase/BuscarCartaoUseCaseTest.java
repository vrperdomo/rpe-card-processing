package br.com.rpe.cartao.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import br.com.rpe.cartao.application.port.out.CartaoRepositorio;
import br.com.rpe.cartao.application.port.out.ProdutoClient;
import br.com.rpe.cartao.application.port.out.ProdutoDto;
import br.com.rpe.cartao.application.port.out.StatusProdutoExterno;
import br.com.rpe.cartao.application.seguranca.Solicitante;
import br.com.rpe.cartao.domain.Cartao;
import br.com.rpe.cartao.domain.Pan;
import br.com.rpe.cartao.domain.Validade;
import br.com.rpe.cartao.domain.exception.RecursoNaoEncontradoException;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class BuscarCartaoUseCaseTest {

  private static final Solicitante DONO = Solicitante.deUsuario("admin");

  private static final Instant AGORA = Instant.parse("2026-09-20T12:00:00Z");

  private final CartaoRepositorio cartaoRepositorio = mock(CartaoRepositorio.class);
  private final ProdutoClient produtoClient = mock(ProdutoClient.class);
  private final BuscarCartaoUseCase useCase =
      new BuscarCartaoUseCase(new AcessoAoCartao(cartaoRepositorio), produtoClient);

  private Cartao cartao(UUID produtoId) {
    return Cartao.emitir(
        UUID.randomUUID(),
        produtoId,
        Pan.of("4532015112830366"),
        "VICTOR RODRIGUES",
        Validade.gerar(AGORA),
        "admin",
        AGORA);
  }

  @Test
  void deveRetornarCartaoComProduto() {
    UUID produtoId = UUID.randomUUID();
    Cartao cartao = cartao(produtoId);
    ProdutoDto produtoDto =
        new ProdutoDto(produtoId, "Gold", "GOLD", "453201", StatusProdutoExterno.ATIVO);
    when(cartaoRepositorio.buscarPorId(cartao.getId())).thenReturn(Optional.of(cartao));
    when(produtoClient.buscarPorId(produtoId)).thenReturn(Optional.of(produtoDto));

    CartaoComProduto resultado = useCase.executar(cartao.getId(), DONO);

    assertThat(resultado.cartao()).isEqualTo(cartao);
    assertThat(resultado.produto()).contains(produtoDto);
  }

  @Test
  void deveRetornarProdutoVazioQuandoProdutoNaoEncontrado() {
    UUID produtoId = UUID.randomUUID();
    Cartao cartao = cartao(produtoId);
    when(cartaoRepositorio.buscarPorId(cartao.getId())).thenReturn(Optional.of(cartao));
    when(produtoClient.buscarPorId(produtoId)).thenReturn(Optional.empty());

    CartaoComProduto resultado = useCase.executar(cartao.getId(), DONO);

    assertThat(resultado.produto()).isEmpty();
  }

  @Test
  void deveLancarRecursoNaoEncontradoQuandoCartaoAusente() {
    UUID id = UUID.randomUUID();
    when(cartaoRepositorio.buscarPorId(id)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> useCase.executar(id, DONO))
        .isInstanceOf(RecursoNaoEncontradoException.class);
  }

  @Test
  void deveLancarRecursoNaoEncontradoQuandoSolicitanteNaoEDono() {
    Cartao cartao = cartao(UUID.randomUUID());
    when(cartaoRepositorio.buscarPorId(cartao.getId())).thenReturn(Optional.of(cartao));

    assertThatThrownBy(() -> useCase.executar(cartao.getId(), Solicitante.deUsuario("outro")))
        .isInstanceOf(RecursoNaoEncontradoException.class);
  }

  @Test
  void deveEntregarQualquerCartaoAoServico() {
    Cartao cartao = cartao(UUID.randomUUID());
    when(cartaoRepositorio.buscarPorId(cartao.getId())).thenReturn(Optional.of(cartao));

    CartaoComProduto resultado =
        useCase.executar(cartao.getId(), Solicitante.deServico("portador-service"));

    assertThat(resultado.cartao()).isEqualTo(cartao);
  }
}

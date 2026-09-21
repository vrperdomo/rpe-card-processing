package br.com.rpe.portador.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import br.com.rpe.portador.application.port.out.CartaoClient;
import br.com.rpe.portador.application.port.out.CartaoDto;
import br.com.rpe.portador.application.port.out.FalhaEmissaoDto;
import br.com.rpe.portador.application.port.out.PortadorRepositorio;
import br.com.rpe.portador.application.port.out.ProdutoClient;
import br.com.rpe.portador.application.port.out.ProdutoDto;
import br.com.rpe.portador.application.port.out.StatusCartaoExterno;
import br.com.rpe.portador.application.port.out.StatusProdutoExterno;
import br.com.rpe.portador.application.seguranca.Solicitante;
import br.com.rpe.portador.application.usecase.PortadorCompleto.StatusEmissao;
import br.com.rpe.portador.domain.Cpf;
import br.com.rpe.portador.domain.Portador;
import br.com.rpe.portador.domain.exception.DependenciaIndisponivelException;
import br.com.rpe.portador.domain.exception.RecursoNaoEncontradoException;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class BuscarPortadorCompletoUseCaseTest {

  private static final Instant AGORA = Instant.parse("2026-09-20T12:00:00Z");
  private static final UUID PRODUTO_ID = UUID.randomUUID();
  private static final Solicitante DONO = Solicitante.deUsuario("admin");

  private final PortadorRepositorio portadorRepositorio = mock(PortadorRepositorio.class);
  private final CartaoClient cartaoClient = mock(CartaoClient.class);
  private final ProdutoClient produtoClient = mock(ProdutoClient.class);
  private final BuscarPortadorCompletoUseCase useCase =
      new BuscarPortadorCompletoUseCase(
          new AcessoAoPortador(portadorRepositorio), cartaoClient, produtoClient);

  private Portador portador() {
    return Portador.cadastrar(
        "Victor", Cpf.of("52998224725"), LocalDate.of(2000, 1, 1), PRODUTO_ID, DONO.id(), AGORA);
  }

  @Test
  void deveRetornarCompletoComEmissaoConcluidaQuandoCartaoExiste() {
    Portador portador = portador();
    CartaoDto cartao =
        new CartaoDto(UUID.randomUUID(), "**** **** **** 1234", "09/31", StatusCartaoExterno.ATIVO);
    ProdutoDto produto = new ProdutoDto(PRODUTO_ID, "Gold", "GOLD", StatusProdutoExterno.ATIVO);
    when(portadorRepositorio.buscarPorId(portador.getId())).thenReturn(Optional.of(portador));
    when(cartaoClient.buscarPorPortadorId(portador.getId())).thenReturn(Optional.of(cartao));
    when(produtoClient.buscarPorId(PRODUTO_ID)).thenReturn(Optional.of(produto));

    PortadorCompleto resultado = useCase.executar(portador.getId(), DONO);

    assertThat(resultado.cartao()).contains(cartao);
    assertThat(resultado.produto()).contains(produto);
    assertThat(resultado.emissao()).isEqualTo(StatusEmissao.CONCLUIDA);
    assertThat(resultado.avisos()).isEmpty();
  }

  @Test
  void deveRetornarEmissaoPendenteQuandoCartaoAindaNaoExiste() {
    Portador portador = portador();
    when(portadorRepositorio.buscarPorId(portador.getId())).thenReturn(Optional.of(portador));
    when(cartaoClient.buscarPorPortadorId(portador.getId())).thenReturn(Optional.empty());
    when(produtoClient.buscarPorId(PRODUTO_ID))
        .thenReturn(
            Optional.of(new ProdutoDto(PRODUTO_ID, "Gold", "GOLD", StatusProdutoExterno.ATIVO)));

    PortadorCompleto resultado = useCase.executar(portador.getId(), DONO);

    assertThat(resultado.cartao()).isEmpty();
    assertThat(resultado.emissao()).isEqualTo(StatusEmissao.PENDENTE);
    assertThat(resultado.avisos()).isEmpty();
  }

  @Test
  void deveDegradarComAvisoQuandoCartaoIndisponivel() {
    Portador portador = portador();
    when(portadorRepositorio.buscarPorId(portador.getId())).thenReturn(Optional.of(portador));
    when(cartaoClient.buscarPorPortadorId(portador.getId()))
        .thenThrow(new DependenciaIndisponivelException("Cartão fora", Duration.ofSeconds(5)));
    when(produtoClient.buscarPorId(PRODUTO_ID))
        .thenReturn(
            Optional.of(new ProdutoDto(PRODUTO_ID, "Gold", "GOLD", StatusProdutoExterno.ATIVO)));

    PortadorCompleto resultado = useCase.executar(portador.getId(), DONO);

    assertThat(resultado.cartao()).isEmpty();
    assertThat(resultado.emissao()).isEqualTo(StatusEmissao.DESCONHECIDA);
    assertThat(resultado.avisos()).containsExactly("Cartão indisponível no momento");
    assertThat(resultado.produto()).isPresent();
  }

  @Test
  void deveDegradarComAvisoQuandoProdutoIndisponivel() {
    Portador portador = portador();
    CartaoDto cartao =
        new CartaoDto(UUID.randomUUID(), "**** **** **** 1234", "09/31", StatusCartaoExterno.ATIVO);
    when(portadorRepositorio.buscarPorId(portador.getId())).thenReturn(Optional.of(portador));
    when(cartaoClient.buscarPorPortadorId(portador.getId())).thenReturn(Optional.of(cartao));
    when(produtoClient.buscarPorId(PRODUTO_ID))
        .thenThrow(new DependenciaIndisponivelException("Produto fora", Duration.ofSeconds(5)));

    PortadorCompleto resultado = useCase.executar(portador.getId(), DONO);

    assertThat(resultado.produto()).isEmpty();
    assertThat(resultado.emissao()).isEqualTo(StatusEmissao.CONCLUIDA);
    assertThat(resultado.avisos()).containsExactly("Produto indisponível no momento");
  }

  @Test
  void deveLancarRecursoNaoEncontradoQuandoPortadorAusente() {
    UUID id = UUID.randomUUID();
    when(portadorRepositorio.buscarPorId(id)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> useCase.executar(id, DONO))
        .isInstanceOf(RecursoNaoEncontradoException.class);
  }

  @Test
  void naoDeveConsultarCartaoNemProdutoQuandoSolicitanteNaoEDono() {
    Portador portador = portador();
    when(portadorRepositorio.buscarPorId(portador.getId())).thenReturn(Optional.of(portador));

    assertThatThrownBy(() -> useCase.executar(portador.getId(), Solicitante.deUsuario("outro")))
        .isInstanceOf(RecursoNaoEncontradoException.class);

    verifyNoInteractions(cartaoClient, produtoClient);
  }

  private void produtoAtivo() {
    when(produtoClient.buscarPorId(PRODUTO_ID))
        .thenReturn(
            Optional.of(new ProdutoDto(PRODUTO_ID, "Gold", "GOLD", StatusProdutoExterno.ATIVO)));
  }

  @Test
  void deveRetornarEmissaoFalhouQuandoNaoHaCartaoMasOCartaoRegistrouFalha() {
    Portador portador = portador();
    FalhaEmissaoDto falha = new FalhaEmissaoDto("Produto inexistente ou não ATIVO", AGORA);
    when(portadorRepositorio.buscarPorId(portador.getId())).thenReturn(Optional.of(portador));
    when(cartaoClient.buscarPorPortadorId(portador.getId())).thenReturn(Optional.empty());
    when(cartaoClient.buscarFalhaEmissao(portador.getId())).thenReturn(Optional.of(falha));
    produtoAtivo();

    PortadorCompleto resultado = useCase.executar(portador.getId(), DONO);

    assertThat(resultado.emissao()).isEqualTo(StatusEmissao.FALHOU);
    assertThat(resultado.falhaEmissao()).contains(falha);
    assertThat(resultado.cartao()).isEmpty();
    assertThat(resultado.avisos()).isEmpty();
  }

  @Test
  void deveManterPendenteQuandoNaoHaCartaoNemFalhaRegistrada() {
    Portador portador = portador();
    when(portadorRepositorio.buscarPorId(portador.getId())).thenReturn(Optional.of(portador));
    when(cartaoClient.buscarPorPortadorId(portador.getId())).thenReturn(Optional.empty());
    when(cartaoClient.buscarFalhaEmissao(portador.getId())).thenReturn(Optional.empty());
    produtoAtivo();

    PortadorCompleto resultado = useCase.executar(portador.getId(), DONO);

    assertThat(resultado.emissao()).isEqualTo(StatusEmissao.PENDENTE);
    assertThat(resultado.falhaEmissao()).isEmpty();
  }

  @Test
  void naoDeveConsultarAFalhaQuandoOCartaoJaExiste() {
    Portador portador = portador();
    CartaoDto cartao =
        new CartaoDto(UUID.randomUUID(), "**** **** **** 1234", "09/31", StatusCartaoExterno.ATIVO);
    when(portadorRepositorio.buscarPorId(portador.getId())).thenReturn(Optional.of(portador));
    when(cartaoClient.buscarPorPortadorId(portador.getId())).thenReturn(Optional.of(cartao));
    produtoAtivo();

    PortadorCompleto resultado = useCase.executar(portador.getId(), DONO);

    assertThat(resultado.emissao()).isEqualTo(StatusEmissao.CONCLUIDA);
    verify(cartaoClient, never()).buscarFalhaEmissao(portador.getId());
  }

  @Test
  void deveDegradarParaDesconhecidaQuandoAConsultaDaFalhaEstaIndisponivel() {
    Portador portador = portador();
    when(portadorRepositorio.buscarPorId(portador.getId())).thenReturn(Optional.of(portador));
    when(cartaoClient.buscarPorPortadorId(portador.getId())).thenReturn(Optional.empty());
    when(cartaoClient.buscarFalhaEmissao(portador.getId()))
        .thenThrow(new DependenciaIndisponivelException("Cartão fora", Duration.ofSeconds(5)));
    produtoAtivo();

    PortadorCompleto resultado = useCase.executar(portador.getId(), DONO);

    // Sem saber o que o Cartão registrou, não dá para afirmar PENDENTE nem FALHOU.
    assertThat(resultado.emissao()).isEqualTo(StatusEmissao.DESCONHECIDA);
    assertThat(resultado.avisos()).containsExactly("Cartão indisponível no momento");
    assertThat(resultado.falhaEmissao()).isEmpty();
  }
}

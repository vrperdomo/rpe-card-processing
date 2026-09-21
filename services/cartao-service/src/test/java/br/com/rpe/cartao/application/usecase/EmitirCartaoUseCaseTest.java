package br.com.rpe.cartao.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import br.com.rpe.cartao.application.port.out.CartaoRepositorio;
import br.com.rpe.cartao.application.port.out.EmissaoFalhaRepositorio;
import br.com.rpe.cartao.application.port.out.MensagemProcessadaRepositorio;
import br.com.rpe.cartao.application.port.out.ProdutoClient;
import br.com.rpe.cartao.application.port.out.ProdutoDto;
import br.com.rpe.cartao.application.port.out.StatusProdutoExterno;
import br.com.rpe.cartao.application.seguranca.Solicitante;
import br.com.rpe.cartao.domain.Cartao;
import br.com.rpe.cartao.domain.exception.RegraNegocioException;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class EmitirCartaoUseCaseTest {

  private static final Instant AGORA = Instant.parse("2026-09-20T12:00:00Z");
  private static final Clock RELOGIO = Clock.fixed(AGORA, ZoneOffset.UTC);

  private final CartaoRepositorio cartaoRepositorio = mock(CartaoRepositorio.class);
  private final EmissaoFalhaRepositorio emissaoFalhaRepositorio =
      mock(EmissaoFalhaRepositorio.class);
  private final MensagemProcessadaRepositorio mensagemProcessadaRepositorio =
      mock(MensagemProcessadaRepositorio.class);
  private final ProdutoClient produtoClient = mock(ProdutoClient.class);
  private final SimpleMeterRegistry meterRegistry = new SimpleMeterRegistry();
  private final EmitirCartaoUseCase useCase =
      new EmitirCartaoUseCase(
          cartaoRepositorio,
          emissaoFalhaRepositorio,
          mensagemProcessadaRepositorio,
          produtoClient,
          meterRegistry,
          RELOGIO);

  private ProdutoDto produtoAtivo(UUID produtoId) {
    return new ProdutoDto(produtoId, "Gold", "GOLD", "453201", StatusProdutoExterno.ATIVO);
  }

  @Test
  void deveEmitirCartaoQuandoProdutoAtivo() {
    UUID eventId = UUID.randomUUID();
    UUID portadorId = UUID.randomUUID();
    UUID produtoId = UUID.randomUUID();
    when(mensagemProcessadaRepositorio.jaProcessada(eventId)).thenReturn(false);
    when(cartaoRepositorio.existePorPortadorEProduto(portadorId, produtoId)).thenReturn(false);
    when(produtoClient.buscarPorId(produtoId)).thenReturn(Optional.of(produtoAtivo(produtoId)));
    when(cartaoRepositorio.salvar(any(Cartao.class))).thenAnswer(inv -> inv.getArgument(0));

    useCase.executar(eventId, portadorId, produtoId, "VICTOR RODRIGUES", "admin");

    verify(cartaoRepositorio).salvar(any(Cartao.class));
    verify(mensagemProcessadaRepositorio).marcarProcessada(eventId, AGORA);
    org.assertj.core.api.Assertions.assertThat(meterRegistry.counter("cartao.emitidos").count())
        .isEqualTo(1.0);
  }

  @Test
  void deveIgnorarQuandoMensagemJaProcessada() {
    UUID eventId = UUID.randomUUID();
    when(mensagemProcessadaRepositorio.jaProcessada(eventId)).thenReturn(true);

    useCase.executar(eventId, UUID.randomUUID(), UUID.randomUUID(), "VICTOR", "admin");

    verify(cartaoRepositorio, never()).salvar(any());
    verify(produtoClient, never()).buscarPorId(any());
  }

  @Test
  void deveIgnorarQuandoJaExisteCartaoParaOPar() {
    UUID eventId = UUID.randomUUID();
    UUID portadorId = UUID.randomUUID();
    UUID produtoId = UUID.randomUUID();
    when(mensagemProcessadaRepositorio.jaProcessada(eventId)).thenReturn(false);
    when(cartaoRepositorio.existePorPortadorEProduto(portadorId, produtoId)).thenReturn(true);

    useCase.executar(eventId, portadorId, produtoId, "VICTOR", "admin");

    verify(cartaoRepositorio, never()).salvar(any());
    verify(produtoClient, never()).buscarPorId(any());
    verify(mensagemProcessadaRepositorio).marcarProcessada(eventId, AGORA);
  }

  @Test
  void deveLancarRegraNegocioQuandoProdutoNaoEncontrado() {
    UUID eventId = UUID.randomUUID();
    UUID portadorId = UUID.randomUUID();
    UUID produtoId = UUID.randomUUID();
    when(mensagemProcessadaRepositorio.jaProcessada(eventId)).thenReturn(false);
    when(cartaoRepositorio.existePorPortadorEProduto(portadorId, produtoId)).thenReturn(false);
    when(produtoClient.buscarPorId(produtoId)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> useCase.executar(eventId, portadorId, produtoId, "VICTOR", "admin"))
        .isInstanceOf(RegraNegocioException.class);

    verify(cartaoRepositorio, never()).salvar(any());
    verify(mensagemProcessadaRepositorio, never()).marcarProcessada(any(), any());
  }

  @Test
  void deveLancarRegraNegocioQuandoProdutoCancelado() {
    UUID eventId = UUID.randomUUID();
    UUID portadorId = UUID.randomUUID();
    UUID produtoId = UUID.randomUUID();
    when(mensagemProcessadaRepositorio.jaProcessada(eventId)).thenReturn(false);
    when(cartaoRepositorio.existePorPortadorEProduto(portadorId, produtoId)).thenReturn(false);
    when(produtoClient.buscarPorId(produtoId))
        .thenReturn(
            Optional.of(
                new ProdutoDto(
                    produtoId, "Gold", "GOLD", "453201", StatusProdutoExterno.CANCELADO)));

    assertThatThrownBy(() -> useCase.executar(eventId, portadorId, produtoId, "VICTOR", "admin"))
        .isInstanceOf(RegraNegocioException.class);
  }

  private Cartao emitirECapturar(String criadoPor) {
    UUID eventId = UUID.randomUUID();
    UUID portadorId = UUID.randomUUID();
    UUID produtoId = UUID.randomUUID();
    when(produtoClient.buscarPorId(produtoId)).thenReturn(Optional.of(produtoAtivo(produtoId)));

    useCase.executar(eventId, portadorId, produtoId, "VICTOR", criadoPor);

    ArgumentCaptor<Cartao> captor = ArgumentCaptor.forClass(Cartao.class);
    verify(cartaoRepositorio).salvar(captor.capture());
    return captor.getValue();
  }

  @Test
  void deveGravarQuemCadastrouComoDonoDoCartao() {
    assertThat(emitirECapturar("admin").getCriadoPor()).isEqualTo("admin");
  }

  @Test
  void deveEmitirComDonoLegadoQuandoEventoNaoTrazCriadoPor() {
    assertThat(emitirECapturar(null).getCriadoPor()).isEqualTo(Solicitante.DONO_LEGADO);
  }

  @Test
  void deveEmitirComDonoLegadoQuandoCriadoPorEstaEmBranco() {
    assertThat(emitirECapturar("  ").getCriadoPor()).isEqualTo(Solicitante.DONO_LEGADO);
  }

  @Test
  void deveApagarAFalhaRegistradaQuandoACartaoEEmitido() {
    UUID portadorId = UUID.randomUUID();
    UUID produtoId = UUID.randomUUID();
    when(produtoClient.buscarPorId(produtoId)).thenReturn(Optional.of(produtoAtivo(produtoId)));

    useCase.executar(UUID.randomUUID(), portadorId, produtoId, "VICTOR", "admin");

    verify(emissaoFalhaRepositorio).removerPorPortadorId(portadorId);
  }

  @Test
  void naoDeveMexerNasFalhasQuandoAEmissaoNaoAcontece() {
    UUID portadorId = UUID.randomUUID();
    UUID produtoId = UUID.randomUUID();
    when(produtoClient.buscarPorId(produtoId)).thenReturn(Optional.empty());

    assertThatThrownBy(
            () -> useCase.executar(UUID.randomUUID(), portadorId, produtoId, "VICTOR", "admin"))
        .isInstanceOf(RegraNegocioException.class);

    verify(emissaoFalhaRepositorio, never()).removerPorPortadorId(any());
  }
}

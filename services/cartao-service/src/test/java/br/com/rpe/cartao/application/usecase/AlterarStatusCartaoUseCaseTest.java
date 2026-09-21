package br.com.rpe.cartao.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import br.com.rpe.cartao.application.port.out.CartaoRepositorio;
import br.com.rpe.cartao.application.seguranca.Solicitante;
import br.com.rpe.cartao.domain.Cartao;
import br.com.rpe.cartao.domain.Pan;
import br.com.rpe.cartao.domain.StatusCartao;
import br.com.rpe.cartao.domain.Validade;
import br.com.rpe.cartao.domain.exception.RecursoNaoEncontradoException;
import br.com.rpe.cartao.domain.exception.RegraNegocioException;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class AlterarStatusCartaoUseCaseTest {

  private static final Solicitante DONO = Solicitante.deUsuario("admin");

  private static final Instant AGORA = Instant.parse("2026-09-20T12:00:00Z");

  private final CartaoRepositorio repositorio = mock(CartaoRepositorio.class);
  private final Clock clock = Clock.fixed(AGORA, ZoneOffset.UTC);
  private final AlterarStatusCartaoUseCase useCase =
      new AlterarStatusCartaoUseCase(new AcessoAoCartao(repositorio), repositorio, clock);

  private Cartao cartaoAtivo() {
    return Cartao.emitir(
        UUID.randomUUID(),
        UUID.randomUUID(),
        Pan.of("4532015112830366"),
        "VICTOR RODRIGUES",
        Validade.gerar(AGORA),
        "admin",
        AGORA);
  }

  @Test
  void deveBloquearCartaoAtivo() {
    Cartao cartao = cartaoAtivo();
    when(repositorio.buscarPorId(cartao.getId())).thenReturn(Optional.of(cartao));
    when(repositorio.salvar(any(Cartao.class))).thenAnswer(inv -> inv.getArgument(0));

    Cartao atualizado = useCase.executar(cartao.getId(), StatusCartao.BLOQUEADO, DONO);

    assertThat(atualizado.getStatus()).isEqualTo(StatusCartao.BLOQUEADO);
    assertThat(atualizado.getAtualizadoEm()).isEqualTo(AGORA);
  }

  @Test
  void deveReativarCartaoBloqueado() {
    Cartao cartao = cartaoAtivo();
    cartao.bloquear(AGORA);
    when(repositorio.buscarPorId(cartao.getId())).thenReturn(Optional.of(cartao));
    when(repositorio.salvar(any(Cartao.class))).thenAnswer(inv -> inv.getArgument(0));

    Cartao atualizado = useCase.executar(cartao.getId(), StatusCartao.ATIVO, DONO);

    assertThat(atualizado.getStatus()).isEqualTo(StatusCartao.ATIVO);
  }

  @Test
  void deveCancelarCartao() {
    Cartao cartao = cartaoAtivo();
    when(repositorio.buscarPorId(cartao.getId())).thenReturn(Optional.of(cartao));
    when(repositorio.salvar(any(Cartao.class))).thenAnswer(inv -> inv.getArgument(0));

    Cartao atualizado = useCase.executar(cartao.getId(), StatusCartao.CANCELADO, DONO);

    assertThat(atualizado.getStatus()).isEqualTo(StatusCartao.CANCELADO);
  }

  @Test
  void deveLancarRegraNegocioQuandoCartaoCancelado() {
    Cartao cartao = cartaoAtivo();
    cartao.cancelar(AGORA);
    when(repositorio.buscarPorId(cartao.getId())).thenReturn(Optional.of(cartao));

    assertThatThrownBy(() -> useCase.executar(cartao.getId(), StatusCartao.ATIVO, DONO))
        .isInstanceOf(RegraNegocioException.class);
  }

  @Test
  void deveLancarRecursoNaoEncontradoQuandoCartaoAusente() {
    UUID id = UUID.randomUUID();
    when(repositorio.buscarPorId(id)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> useCase.executar(id, StatusCartao.BLOQUEADO, DONO))
        .isInstanceOf(RecursoNaoEncontradoException.class);
  }

  @Test
  void naoDeveAlterarNemSalvarQuandoSolicitanteNaoEDono() {
    Cartao cartao = cartaoAtivo();
    when(repositorio.buscarPorId(cartao.getId())).thenReturn(Optional.of(cartao));

    assertThatThrownBy(
            () ->
                useCase.executar(
                    cartao.getId(), StatusCartao.CANCELADO, Solicitante.deUsuario("outro")))
        .isInstanceOf(RecursoNaoEncontradoException.class);

    assertThat(cartao.getStatus()).isEqualTo(StatusCartao.ATIVO);
    verify(repositorio, never()).salvar(any(Cartao.class));
  }
}

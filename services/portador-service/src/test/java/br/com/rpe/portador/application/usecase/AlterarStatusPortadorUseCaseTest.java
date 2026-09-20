package br.com.rpe.portador.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import br.com.rpe.portador.application.port.out.PortadorRepositorio;
import br.com.rpe.portador.domain.Cpf;
import br.com.rpe.portador.domain.Portador;
import br.com.rpe.portador.domain.StatusPortador;
import br.com.rpe.portador.domain.exception.RecursoNaoEncontradoException;
import br.com.rpe.portador.domain.exception.RegraNegocioException;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class AlterarStatusPortadorUseCaseTest {

  private static final Instant AGORA = Instant.parse("2026-09-20T12:00:00Z");

  private final PortadorRepositorio repositorio = mock(PortadorRepositorio.class);
  private final Clock clock = Clock.fixed(AGORA, ZoneOffset.UTC);
  private final AlterarStatusPortadorUseCase useCase =
      new AlterarStatusPortadorUseCase(repositorio, clock);

  private Portador portadorAtivo() {
    return Portador.cadastrar(
        "Victor", Cpf.of("52998224725"), LocalDate.of(2000, 1, 1), UUID.randomUUID(), AGORA);
  }

  @Test
  void deveBloquearPortadorAtivo() {
    Portador portador = portadorAtivo();
    when(repositorio.buscarPorId(portador.getId())).thenReturn(Optional.of(portador));
    when(repositorio.salvar(any(Portador.class))).thenAnswer(inv -> inv.getArgument(0));

    Portador atualizado = useCase.executar(portador.getId(), StatusPortador.BLOQUEADO);

    assertThat(atualizado.getStatus()).isEqualTo(StatusPortador.BLOQUEADO);
    assertThat(atualizado.getAtualizadoEm()).isEqualTo(AGORA);
  }

  @Test
  void deveReativarPortadorBloqueado() {
    Portador portador = portadorAtivo();
    portador.bloquear(AGORA);
    when(repositorio.buscarPorId(portador.getId())).thenReturn(Optional.of(portador));
    when(repositorio.salvar(any(Portador.class))).thenAnswer(inv -> inv.getArgument(0));

    Portador atualizado = useCase.executar(portador.getId(), StatusPortador.ATIVO);

    assertThat(atualizado.getStatus()).isEqualTo(StatusPortador.ATIVO);
  }

  @Test
  void deveCancelarPortador() {
    Portador portador = portadorAtivo();
    when(repositorio.buscarPorId(portador.getId())).thenReturn(Optional.of(portador));
    when(repositorio.salvar(any(Portador.class))).thenAnswer(inv -> inv.getArgument(0));

    Portador atualizado = useCase.executar(portador.getId(), StatusPortador.CANCELADO);

    assertThat(atualizado.getStatus()).isEqualTo(StatusPortador.CANCELADO);
  }

  @Test
  void deveLancarRegraNegocioQuandoTransicaoInvalida() {
    Portador portador = portadorAtivo();
    portador.cancelar(AGORA);
    when(repositorio.buscarPorId(portador.getId())).thenReturn(Optional.of(portador));

    assertThatThrownBy(() -> useCase.executar(portador.getId(), StatusPortador.ATIVO))
        .isInstanceOf(RegraNegocioException.class);
  }

  @Test
  void deveLancarRecursoNaoEncontradoQuandoPortadorAusente() {
    UUID id = UUID.randomUUID();
    when(repositorio.buscarPorId(id)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> useCase.executar(id, StatusPortador.BLOQUEADO))
        .isInstanceOf(RecursoNaoEncontradoException.class);
  }
}

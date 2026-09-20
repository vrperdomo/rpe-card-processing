package br.com.rpe.portador.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import br.com.rpe.portador.application.port.out.PortadorRepositorio;
import br.com.rpe.portador.domain.Cpf;
import br.com.rpe.portador.domain.Portador;
import br.com.rpe.portador.domain.exception.RecursoNaoEncontradoException;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class BuscarPortadorUseCaseTest {

  private final PortadorRepositorio repositorio = mock(PortadorRepositorio.class);
  private final BuscarPortadorUseCase useCase = new BuscarPortadorUseCase(repositorio);

  @Test
  void deveRetornarPortadorQuandoEncontrado() {
    Portador portador =
        Portador.cadastrar(
            "Victor",
            Cpf.of("52998224725"),
            LocalDate.of(2000, 1, 1),
            UUID.randomUUID(),
            Instant.parse("2026-09-20T12:00:00Z"));
    when(repositorio.buscarPorId(portador.getId())).thenReturn(Optional.of(portador));

    Portador encontrado = useCase.executar(portador.getId());

    assertThat(encontrado).isEqualTo(portador);
  }

  @Test
  void deveLancarRecursoNaoEncontradoQuandoAusente() {
    UUID id = UUID.randomUUID();
    when(repositorio.buscarPorId(id)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> useCase.executar(id))
        .isInstanceOf(RecursoNaoEncontradoException.class);
  }
}

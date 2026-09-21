package br.com.rpe.cartao.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import br.com.rpe.cartao.application.port.out.EmissaoFalhaRepositorio;
import br.com.rpe.cartao.application.seguranca.Solicitante;
import br.com.rpe.cartao.domain.EmissaoFalha;
import br.com.rpe.cartao.domain.exception.RecursoNaoEncontradoException;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class BuscarFalhaEmissaoUseCaseTest {

  private static final Instant AGORA = Instant.parse("2026-09-21T10:00:00Z");

  private final EmissaoFalhaRepositorio repositorio = mock(EmissaoFalhaRepositorio.class);
  private final BuscarFalhaEmissaoUseCase useCase = new BuscarFalhaEmissaoUseCase(repositorio);

  private EmissaoFalha falhaDe(UUID portadorId, String dono) {
    return new EmissaoFalha(portadorId, UUID.randomUUID(), "Produto inexistente", dono, AGORA);
  }

  @Test
  void deveEntregarAFalhaAoDono() {
    UUID portadorId = UUID.randomUUID();
    EmissaoFalha falha = falhaDe(portadorId, "admin");
    when(repositorio.buscarPorPortadorId(portadorId)).thenReturn(Optional.of(falha));

    assertThat(useCase.executar(portadorId, Solicitante.deUsuario("admin"))).isEqualTo(falha);
  }

  @Test
  void deveEntregarQualquerFalhaAoServico() {
    UUID portadorId = UUID.randomUUID();
    EmissaoFalha falha = falhaDe(portadorId, "admin");
    when(repositorio.buscarPorPortadorId(portadorId)).thenReturn(Optional.of(falha));

    assertThat(useCase.executar(portadorId, Solicitante.deServico("portador-service")))
        .isEqualTo(falha);
  }

  @Test
  void deveResponderNaoEncontradoParaQuemNaoEDonoIgualAoDeFalhaInexistente() {
    UUID portadorId = UUID.randomUUID();
    UUID semFalha = UUID.randomUUID();
    when(repositorio.buscarPorPortadorId(portadorId))
        .thenReturn(Optional.of(falhaDe(portadorId, "admin")));
    when(repositorio.buscarPorPortadorId(semFalha)).thenReturn(Optional.empty());
    Solicitante outro = Solicitante.deUsuario("outro");

    var alheia =
        org.junit.jupiter.api.Assertions.assertThrows(
            RecursoNaoEncontradoException.class, () -> useCase.executar(portadorId, outro));
    var ausente =
        org.junit.jupiter.api.Assertions.assertThrows(
            RecursoNaoEncontradoException.class, () -> useCase.executar(semFalha, outro));

    assertThat(alheia.getMessage())
        .isEqualTo("Nenhuma falha de emissão registrada para o portador %s".formatted(portadorId));
    assertThat(ausente.getMessage())
        .isEqualTo("Nenhuma falha de emissão registrada para o portador %s".formatted(semFalha));
  }

  @Test
  void naoDeveEntregarFalhaLegadaAUsuario() {
    UUID portadorId = UUID.randomUUID();
    when(repositorio.buscarPorPortadorId(portadorId))
        .thenReturn(Optional.of(falhaDe(portadorId, Solicitante.DONO_LEGADO)));

    org.junit.jupiter.api.Assertions.assertThrows(
        RecursoNaoEncontradoException.class,
        () -> useCase.executar(portadorId, Solicitante.deUsuario("admin")));
  }
}

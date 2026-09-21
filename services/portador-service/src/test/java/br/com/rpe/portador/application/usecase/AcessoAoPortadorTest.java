package br.com.rpe.portador.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import br.com.rpe.portador.application.port.out.PortadorRepositorio;
import br.com.rpe.portador.application.seguranca.Solicitante;
import br.com.rpe.portador.domain.Cpf;
import br.com.rpe.portador.domain.Portador;
import br.com.rpe.portador.domain.StatusPortador;
import br.com.rpe.portador.domain.exception.RecursoNaoEncontradoException;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class AcessoAoPortadorTest {

  private static final Instant AGORA = Instant.parse("2026-09-21T10:00:00Z");

  private final PortadorRepositorio repositorio = mock(PortadorRepositorio.class);
  private final AcessoAoPortador acesso = new AcessoAoPortador(repositorio);

  private Portador portadorDe(String dono) {
    return Portador.reconstituir(
        UUID.randomUUID(),
        "Victor",
        Cpf.of("52998224725"),
        LocalDate.of(2000, 1, 1),
        UUID.randomUUID(),
        dono,
        StatusPortador.ATIVO,
        AGORA,
        AGORA);
  }

  @Test
  void deveEntregarOPortadorAoDono() {
    Portador portador = portadorDe("admin");
    when(repositorio.buscarPorId(portador.getId())).thenReturn(Optional.of(portador));

    assertThat(acesso.obter(portador.getId(), Solicitante.deUsuario("admin"))).isSameAs(portador);
  }

  @Test
  void deveEntregarQualquerPortadorAoServico() {
    Portador portador = portadorDe("admin");
    when(repositorio.buscarPorId(portador.getId())).thenReturn(Optional.of(portador));

    assertThat(acesso.obter(portador.getId(), Solicitante.deServico("cartao-service")))
        .isSameAs(portador);
  }

  @Test
  void deveResponderNaoEncontradoParaQuemNaoEDonoSemRevelarQueOIdExiste() {
    Portador portador = portadorDe("admin");
    UUID inexistente = UUID.randomUUID();
    when(repositorio.buscarPorId(portador.getId())).thenReturn(Optional.of(portador));
    when(repositorio.buscarPorId(inexistente)).thenReturn(Optional.empty());
    Solicitante outro = Solicitante.deUsuario("outro");

    var alheio =
        org.junit.jupiter.api.Assertions.assertThrows(
            RecursoNaoEncontradoException.class, () -> acesso.obter(portador.getId(), outro));
    var ausente =
        org.junit.jupiter.api.Assertions.assertThrows(
            RecursoNaoEncontradoException.class, () -> acesso.obter(inexistente, outro));

    // Mesma exceção e mesmo formato de mensagem: o cliente não distingue "alheio" de "inexistente".
    assertThat(alheio.getMessage())
        .isEqualTo("Portador %s não encontrado".formatted(portador.getId()));
    assertThat(ausente.getMessage()).isEqualTo("Portador %s não encontrado".formatted(inexistente));
  }

  @Test
  void naoDeveEntregarPortadorLegadoAUsuario() {
    Portador portador = portadorDe(Solicitante.DONO_LEGADO);
    when(repositorio.buscarPorId(portador.getId())).thenReturn(Optional.of(portador));

    assertThatThrownBy(() -> acesso.obter(portador.getId(), Solicitante.deUsuario("admin")))
        .isInstanceOf(RecursoNaoEncontradoException.class);
  }
}

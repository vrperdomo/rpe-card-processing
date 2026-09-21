package br.com.rpe.cartao.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import br.com.rpe.cartao.application.port.out.CartaoRepositorio;
import br.com.rpe.cartao.application.seguranca.Solicitante;
import br.com.rpe.cartao.domain.Cartao;
import br.com.rpe.cartao.domain.Pan;
import br.com.rpe.cartao.domain.Validade;
import br.com.rpe.cartao.domain.exception.RecursoNaoEncontradoException;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class AcessoAoCartaoTest {

  private static final Instant AGORA = Instant.parse("2026-09-21T10:00:00Z");

  private final CartaoRepositorio repositorio = mock(CartaoRepositorio.class);
  private final AcessoAoCartao acesso = new AcessoAoCartao(repositorio);

  private Cartao cartaoDe(String dono) {
    return Cartao.emitir(
        UUID.randomUUID(),
        UUID.randomUUID(),
        Pan.of("4532015112830366"),
        "VICTOR RODRIGUES",
        Validade.gerar(AGORA),
        dono,
        AGORA);
  }

  @Test
  void deveEntregarOCartaoAoDono() {
    Cartao cartao = cartaoDe("admin");
    when(repositorio.buscarPorId(cartao.getId())).thenReturn(Optional.of(cartao));

    assertThat(acesso.obter(cartao.getId(), Solicitante.deUsuario("admin"))).isSameAs(cartao);
  }

  @Test
  void deveEntregarQualquerCartaoAoServico() {
    Cartao cartao = cartaoDe("admin");
    when(repositorio.buscarPorId(cartao.getId())).thenReturn(Optional.of(cartao));

    assertThat(acesso.obter(cartao.getId(), Solicitante.deServico("portador-service")))
        .isSameAs(cartao);
  }

  @Test
  void deveResponderNaoEncontradoParaQuemNaoEDonoSemRevelarQueOIdExiste() {
    Cartao cartao = cartaoDe("admin");
    UUID inexistente = UUID.randomUUID();
    when(repositorio.buscarPorId(cartao.getId())).thenReturn(Optional.of(cartao));
    when(repositorio.buscarPorId(inexistente)).thenReturn(Optional.empty());
    Solicitante outro = Solicitante.deUsuario("outro");

    var alheio =
        org.junit.jupiter.api.Assertions.assertThrows(
            RecursoNaoEncontradoException.class, () -> acesso.obter(cartao.getId(), outro));
    var ausente =
        org.junit.jupiter.api.Assertions.assertThrows(
            RecursoNaoEncontradoException.class, () -> acesso.obter(inexistente, outro));

    // Mesma exceção e mesmo formato de mensagem: o cliente não distingue "alheio" de "inexistente".
    assertThat(alheio.getMessage()).isEqualTo("Cartão %s não encontrado".formatted(cartao.getId()));
    assertThat(ausente.getMessage()).isEqualTo("Cartão %s não encontrado".formatted(inexistente));
  }

  @Test
  void naoDeveEntregarCartaoLegadoAUsuario() {
    Cartao cartao = cartaoDe(Solicitante.DONO_LEGADO);
    when(repositorio.buscarPorId(cartao.getId())).thenReturn(Optional.of(cartao));

    assertThatThrownBy(() -> acesso.obter(cartao.getId(), Solicitante.deUsuario("admin")))
        .isInstanceOf(RecursoNaoEncontradoException.class);
  }
}

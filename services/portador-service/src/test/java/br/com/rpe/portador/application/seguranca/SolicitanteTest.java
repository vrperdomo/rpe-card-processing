package br.com.rpe.portador.application.seguranca;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class SolicitanteTest {

  @Test
  void usuarioDeveAcessarApenasOQueCriou() {
    Solicitante usuario = Solicitante.deUsuario("admin");

    assertThat(usuario.podeAcessar("admin")).isTrue();
    assertThat(usuario.podeAcessar("outro")).isFalse();
  }

  @Test
  void usuarioNaoDeveAcessarRecursoSemDonoOuLegado() {
    Solicitante usuario = Solicitante.deUsuario("admin");

    assertThat(usuario.podeAcessar(null)).isFalse();
    assertThat(usuario.podeAcessar(Solicitante.DONO_LEGADO)).isFalse();
  }

  @Test
  void usuarioChamadoLegadoNaoDeveAcessarRecursoLegado() {
    // Falha fechada: mesmo que um usuário se chame como o sentinela, não herda as linhas antigas.
    assertThat(Solicitante.deUsuario("legado").podeAcessar(Solicitante.DONO_LEGADO)).isFalse();
  }

  @Test
  void servicoDeveAcessarQualquerRecurso() {
    Solicitante servico = Solicitante.deServico("portador-service");

    assertThat(servico.podeAcessar("admin")).isTrue();
    assertThat(servico.podeAcessar(Solicitante.DONO_LEGADO)).isTrue();
  }

  @Test
  void deveRejeitarIdVazio() {
    assertThatThrownBy(() -> Solicitante.deUsuario(" "))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> Solicitante.deUsuario(null))
        .isInstanceOf(IllegalArgumentException.class);
  }
}

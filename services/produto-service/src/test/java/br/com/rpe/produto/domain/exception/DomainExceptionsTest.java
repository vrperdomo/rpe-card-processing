package br.com.rpe.produto.domain.exception;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import org.junit.jupiter.api.Test;

class DomainExceptionsTest {

  @Test
  void recursoNaoEncontradoExceptionDeveManterMensagem() {
    var excecao = new RecursoNaoEncontradoException("produto não encontrado");

    assertThat(excecao).isInstanceOf(DomainException.class).hasMessage("produto não encontrado");
  }

  @Test
  void conflitoExceptionDeveManterMensagem() {
    var excecao = new ConflitoException("nome de produto já cadastrado");

    assertThat(excecao)
        .isInstanceOf(DomainException.class)
        .hasMessage("nome de produto já cadastrado");
  }

  @Test
  void dependenciaIndisponivelExceptionDeveManterMensagemERetryAfter() {
    var retryAfter = Duration.ofSeconds(30);

    var excecao = new DependenciaIndisponivelException("produto service indisponível", retryAfter);

    assertThat(excecao)
        .isInstanceOf(DomainException.class)
        .hasMessage("produto service indisponível");
    assertThat(excecao.getRetryAfter()).isEqualTo(retryAfter);
  }
}

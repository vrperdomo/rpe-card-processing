package br.com.rpe.portador.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import br.com.rpe.portador.application.port.out.GeradorToken;
import br.com.rpe.portador.application.port.out.VerificadorSenha;
import br.com.rpe.portador.config.UsuarioSeedProperties;
import br.com.rpe.portador.domain.exception.CredenciaisInvalidasException;
import org.junit.jupiter.api.Test;

class AutenticarUseCaseTest {

  private static final UsuarioSeedProperties USUARIO_SEED =
      new UsuarioSeedProperties("admin", "hash-bcrypt-qualquer");

  private final VerificadorSenha verificadorSenha = mock(VerificadorSenha.class);
  private final GeradorToken geradorToken = mock(GeradorToken.class);
  private final AutenticarUseCase useCase =
      new AutenticarUseCase(USUARIO_SEED, verificadorSenha, geradorToken);

  @Test
  void deveEmitirTokenQuandoCredenciaisCorretas() {
    GeradorToken.Token tokenEsperado = new GeradorToken.Token("jwt-gerado", 1800);
    when(verificadorSenha.confere("senha-correta", USUARIO_SEED.passwordHash())).thenReturn(true);
    when(geradorToken.gerar(USUARIO_SEED.username())).thenReturn(tokenEsperado);

    GeradorToken.Token resultado = useCase.executar("admin", "senha-correta");

    assertThat(resultado).isEqualTo(tokenEsperado);
  }

  @Test
  void deveLancarCredenciaisInvalidasQuandoUsuarioDiferente() {
    assertThatThrownBy(() -> useCase.executar("outro-usuario", "qualquer"))
        .isInstanceOf(CredenciaisInvalidasException.class);
  }

  @Test
  void deveLancarCredenciaisInvalidasQuandoSenhaIncorreta() {
    when(verificadorSenha.confere("senha-errada", USUARIO_SEED.passwordHash())).thenReturn(false);

    assertThatThrownBy(() -> useCase.executar("admin", "senha-errada"))
        .isInstanceOf(CredenciaisInvalidasException.class);
  }
}

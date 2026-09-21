package br.com.rpe.portador.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import br.com.rpe.portador.application.port.out.ControleTentativasLogin;
import br.com.rpe.portador.application.port.out.GeradorToken;
import br.com.rpe.portador.application.port.out.VerificadorSenha;
import br.com.rpe.portador.config.UsuarioSeedProperties;
import br.com.rpe.portador.domain.exception.CredenciaisInvalidasException;
import br.com.rpe.portador.domain.exception.LimiteTentativasExcedidoException;
import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;

class AutenticarUseCaseTest {

  private static final String ORIGEM = "10.0.0.1";
  private static final UsuarioSeedProperties USUARIO_SEED =
      new UsuarioSeedProperties("admin", "hash-bcrypt-qualquer");

  private final VerificadorSenha verificadorSenha = mock(VerificadorSenha.class);
  private final GeradorToken geradorToken = mock(GeradorToken.class);
  private final ControleTentativasLogin controleTentativas = mock(ControleTentativasLogin.class);
  private final AutenticarUseCase useCase =
      new AutenticarUseCase(USUARIO_SEED, verificadorSenha, geradorToken, controleTentativas);

  @Test
  void deveEmitirTokenQuandoCredenciaisCorretas() {
    GeradorToken.Token tokenEsperado = new GeradorToken.Token("jwt-gerado", 1800);
    when(verificadorSenha.confere("senha-correta", USUARIO_SEED.passwordHash())).thenReturn(true);
    when(geradorToken.gerar(USUARIO_SEED.username())).thenReturn(tokenEsperado);

    GeradorToken.Token resultado = useCase.executar("admin", "senha-correta", ORIGEM);

    assertThat(resultado).isEqualTo(tokenEsperado);
  }

  @Test
  void deveZerarFalhasDaOrigemQuandoLoginTemSucesso() {
    when(verificadorSenha.confere("senha-correta", USUARIO_SEED.passwordHash())).thenReturn(true);
    when(geradorToken.gerar(USUARIO_SEED.username())).thenReturn(new GeradorToken.Token("jwt", 1));

    useCase.executar("admin", "senha-correta", ORIGEM);

    InOrder ordem = inOrder(controleTentativas);
    ordem.verify(controleTentativas).registrarTentativa(ORIGEM);
    ordem.verify(controleTentativas).registrarSucesso(ORIGEM);
  }

  @Test
  void deveLancarCredenciaisInvalidasSemZerarTentativasQuandoUsuarioDiferente() {
    assertThatThrownBy(() -> useCase.executar("outro-usuario", "qualquer", ORIGEM))
        .isInstanceOf(CredenciaisInvalidasException.class);

    verify(controleTentativas).registrarTentativa(ORIGEM);
    verify(controleTentativas, never()).registrarSucesso(anyString());
  }

  @Test
  void deveLancarCredenciaisInvalidasSemZerarTentativasQuandoSenhaIncorreta() {
    when(verificadorSenha.confere("senha-errada", USUARIO_SEED.passwordHash())).thenReturn(false);

    assertThatThrownBy(() -> useCase.executar("admin", "senha-errada", ORIGEM))
        .isInstanceOf(CredenciaisInvalidasException.class);

    verify(controleTentativas).registrarTentativa(ORIGEM);
    verify(controleTentativas, never()).registrarSucesso(anyString());
  }

  @Test
  void naoDeveVerificarSenhaNemEmitirTokenQuandoOrigemEstaBloqueada() {
    doThrow(new LimiteTentativasExcedidoException("bloqueado", Duration.ofSeconds(30)))
        .when(controleTentativas)
        .registrarTentativa(ORIGEM);

    assertThatThrownBy(() -> useCase.executar("admin", "senha-correta", ORIGEM))
        .isInstanceOf(LimiteTentativasExcedidoException.class);

    verifyNoInteractions(verificadorSenha, geradorToken);
    verify(controleTentativas, never()).registrarSucesso(any());
  }
}

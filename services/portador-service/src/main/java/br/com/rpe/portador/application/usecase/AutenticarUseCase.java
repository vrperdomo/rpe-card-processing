package br.com.rpe.portador.application.usecase;

import br.com.rpe.portador.application.port.out.GeradorToken;
import br.com.rpe.portador.application.port.out.VerificadorSenha;
import br.com.rpe.portador.config.UsuarioSeedProperties;
import br.com.rpe.portador.domain.exception.CredenciaisInvalidasException;
import org.springframework.stereotype.Service;

@Service
public class AutenticarUseCase {

  private static final String MENSAGEM_CREDENCIAIS_INVALIDAS = "Usuário ou senha inválidos";

  private final UsuarioSeedProperties usuarioSeed;
  private final VerificadorSenha verificadorSenha;
  private final GeradorToken geradorToken;

  public AutenticarUseCase(
      UsuarioSeedProperties usuarioSeed,
      VerificadorSenha verificadorSenha,
      GeradorToken geradorToken) {
    this.usuarioSeed = usuarioSeed;
    this.verificadorSenha = verificadorSenha;
    this.geradorToken = geradorToken;
  }

  public GeradorToken.Token executar(String username, String senha) {
    if (!usuarioSeed.username().equals(username)) {
      throw new CredenciaisInvalidasException(MENSAGEM_CREDENCIAIS_INVALIDAS);
    }
    if (!verificadorSenha.confere(senha, usuarioSeed.passwordHash())) {
      throw new CredenciaisInvalidasException(MENSAGEM_CREDENCIAIS_INVALIDAS);
    }
    return geradorToken.gerar(usuarioSeed.username());
  }
}

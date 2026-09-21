package br.com.rpe.portador.application.usecase;

import br.com.rpe.portador.application.port.out.ControleTentativasLogin;
import br.com.rpe.portador.application.port.out.GeradorToken;
import br.com.rpe.portador.application.port.out.VerificadorSenha;
import br.com.rpe.portador.config.UsuarioSeedProperties;
import br.com.rpe.portador.domain.exception.CredenciaisInvalidasException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

// ADR-009 (OWASP A04/A07/A09): o login limita tentativas por origem (ControleTentativasLogin, que
// reserva a tentativa antes de verificar a senha) e registra toda falha. O username digitado NAO
// entra no log: e entrada do cliente (injecao de log) e pode ser, por engano, um CPF - que nunca
// deve ser logado (CLAUDE.md, regra 4).
@Service
public class AutenticarUseCase {

  private static final Logger log = LoggerFactory.getLogger(AutenticarUseCase.class);
  private static final String MENSAGEM_CREDENCIAIS_INVALIDAS = "Usuário ou senha inválidos";

  private final UsuarioSeedProperties usuarioSeed;
  private final VerificadorSenha verificadorSenha;
  private final GeradorToken geradorToken;
  private final ControleTentativasLogin controleTentativas;

  public AutenticarUseCase(
      UsuarioSeedProperties usuarioSeed,
      VerificadorSenha verificadorSenha,
      GeradorToken geradorToken,
      ControleTentativasLogin controleTentativas) {
    this.usuarioSeed = usuarioSeed;
    this.verificadorSenha = verificadorSenha;
    this.geradorToken = geradorToken;
    this.controleTentativas = controleTentativas;
  }

  public GeradorToken.Token executar(String username, String senha, String origem) {
    controleTentativas.registrarTentativa(origem);
    String motivoFalha = motivoDaFalha(username, senha);
    if (motivoFalha != null) {
      log.warn("Falha de login: motivo={} origem={}", motivoFalha, origem);
      throw new CredenciaisInvalidasException(MENSAGEM_CREDENCIAIS_INVALIDAS);
    }
    controleTentativas.registrarSucesso(origem);
    return geradorToken.gerar(usuarioSeed.username());
  }

  private String motivoDaFalha(String username, String senha) {
    if (!usuarioSeed.username().equals(username)) {
      return "usuario-inexistente";
    }
    if (!verificadorSenha.confere(senha, usuarioSeed.passwordHash())) {
      return "senha-incorreta";
    }
    return null;
  }
}

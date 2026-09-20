package br.com.rpe.portador.adapters.out.security;

import br.com.rpe.portador.application.port.out.VerificadorSenha;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class BCryptVerificadorSenha implements VerificadorSenha {

  private final PasswordEncoder passwordEncoder;

  public BCryptVerificadorSenha(PasswordEncoder passwordEncoder) {
    this.passwordEncoder = passwordEncoder;
  }

  @Override
  public boolean confere(String senhaBruta, String hashArmazenado) {
    return passwordEncoder.matches(senhaBruta, hashArmazenado);
  }
}

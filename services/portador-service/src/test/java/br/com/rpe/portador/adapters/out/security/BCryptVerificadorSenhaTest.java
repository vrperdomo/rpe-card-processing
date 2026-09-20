package br.com.rpe.portador.adapters.out.security;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

class BCryptVerificadorSenhaTest {

  private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
  private final BCryptVerificadorSenha verificadorSenha =
      new BCryptVerificadorSenha(passwordEncoder);

  @Test
  void deveConfirmarQuandoSenhaBateComHash() {
    String hash = passwordEncoder.encode("senha-correta");

    assertThat(verificadorSenha.confere("senha-correta", hash)).isTrue();
  }

  @Test
  void deveNegarQuandoSenhaNaoBateComHash() {
    String hash = passwordEncoder.encode("senha-correta");

    assertThat(verificadorSenha.confere("senha-errada", hash)).isFalse();
  }
}

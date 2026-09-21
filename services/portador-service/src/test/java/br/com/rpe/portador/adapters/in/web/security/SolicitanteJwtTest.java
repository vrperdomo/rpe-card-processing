package br.com.rpe.portador.adapters.in.web.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.com.rpe.portador.application.seguranca.Solicitante;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.oauth2.jwt.Jwt;

class SolicitanteJwtTest {

  private Jwt.Builder token(String subject) {
    return Jwt.withTokenValue("token")
        .header("alg", "HS256")
        .subject(subject)
        .issuedAt(Instant.parse("2026-09-21T10:00:00Z"))
        .expiresAt(Instant.parse("2026-09-21T10:30:00Z"));
  }

  @Test
  void tokenDeUsuarioDeveVirarSolicitanteDeUsuarioComOSub() {
    Solicitante solicitante = SolicitanteJwt.de(token("admin").build());

    assertThat(solicitante).isEqualTo(Solicitante.deUsuario("admin"));
  }

  @Test
  void tokenComEscopoServicoDeveVirarSolicitanteDeServico() {
    Solicitante solicitante =
        SolicitanteJwt.de(token("portador-service").claim("scope", "servico").build());

    assertThat(solicitante).isEqualTo(Solicitante.deServico("portador-service"));
  }

  @Test
  void escopoServicoDeveSerReconhecidoEntreVariosEscopos() {
    Solicitante solicitante =
        SolicitanteJwt.de(token("portador-service").claim("scope", "read servico").build());

    assertThat(solicitante.servico()).isTrue();
  }

  @Test
  void outroEscopoNaoDeveTornarOTokenDeServico() {
    Solicitante solicitante = SolicitanteJwt.de(token("admin").claim("scope", "read").build());

    assertThat(solicitante.servico()).isFalse();
  }

  @Test
  void deveNegarAcessoQuandoTokenNaoTemSub() {
    Jwt semSub = Jwt.withTokenValue("token").header("alg", "HS256").claim("scope", "read").build();

    assertThatThrownBy(() -> SolicitanteJwt.de(semSub)).isInstanceOf(AccessDeniedException.class);
  }
}

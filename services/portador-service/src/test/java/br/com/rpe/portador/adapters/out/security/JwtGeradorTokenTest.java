package br.com.rpe.portador.adapters.out.security;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.rpe.portador.application.port.out.GeradorToken;
import br.com.rpe.portador.config.JwtProperties;
import com.nimbusds.jose.crypto.MACVerifier;
import com.nimbusds.jose.jwk.source.ImmutableSecret;
import com.nimbusds.jwt.SignedJWT;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import javax.crypto.spec.SecretKeySpec;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

class JwtGeradorTokenTest {

  private static final String SECRET = "segredo-de-teste-com-pelo-menos-32-caracteres";
  private static final String ISSUER = "https://portador-service.rpe.local";
  private static final String AUDIENCE = "rpe-api";
  private static final Instant AGORA = Instant.parse("2026-09-19T10:00:00Z");

  private final JwtProperties properties =
      new JwtProperties(SECRET, ISSUER, AUDIENCE, Duration.ofMinutes(30));
  private final Clock clock = Clock.fixed(AGORA, ZoneOffset.UTC);
  private final NimbusJwtEncoder jwtEncoder =
      new NimbusJwtEncoder(
          new ImmutableSecret<>(
              new SecretKeySpec(SECRET.getBytes(StandardCharsets.UTF_8), "HmacSHA256")));
  private final JwtGeradorToken geradorToken = new JwtGeradorToken(jwtEncoder, properties, clock);

  @Test
  void deveGerarTokenComClaimsEExpiracaoCorretos() throws Exception {
    GeradorToken.Token token = geradorToken.gerar("admin");

    assertThat(token.expiraEmSegundos()).isEqualTo(1800);

    SignedJWT jwt = SignedJWT.parse(token.valor());
    assertThat(jwt.verify(new MACVerifier(SECRET.getBytes(StandardCharsets.UTF_8)))).isTrue();
    var claims = jwt.getJWTClaimsSet();
    assertThat(claims.getSubject()).isEqualTo("admin");
    assertThat(claims.getIssuer()).isEqualTo(ISSUER);
    assertThat(claims.getAudience()).containsExactly(AUDIENCE);
    assertThat(claims.getIssueTime().toInstant()).isEqualTo(AGORA);
    assertThat(claims.getExpirationTime().toInstant())
        .isEqualTo(AGORA.plus(Duration.ofMinutes(30)));
  }

  @Test
  void deveExigirMacAlgorithmHS256() throws Exception {
    GeradorToken.Token token = geradorToken.gerar("admin");

    SignedJWT jwt = SignedJWT.parse(token.valor());
    assertThat(jwt.getHeader().getAlgorithm().getName()).isEqualTo(MacAlgorithm.HS256.getName());
  }
}

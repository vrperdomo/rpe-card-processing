package br.com.rpe.cartao.adapters.out.http;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.rpe.cartao.config.JwtProperties;
import com.nimbusds.jose.jwk.source.ImmutableSecret;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import javax.crypto.spec.SecretKeySpec;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

class EmissorTokenServicoTest {

  private static final JwtProperties PROPERTIES =
      new JwtProperties(
          "segredo-de-teste-com-32-caracteres!", "https://cartao.rpe.local", "rpe-api");
  private static final Instant AGORA = Instant.now().truncatedTo(ChronoUnit.SECONDS);

  private final EmissorTokenServico emissor =
      new EmissorTokenServico(
          new NimbusJwtEncoder(
              new ImmutableSecret<>(
                  new SecretKeySpec(
                      PROPERTIES.secret().getBytes(StandardCharsets.UTF_8), "HmacSHA256"))),
          PROPERTIES,
          Clock.fixed(AGORA, ZoneOffset.UTC));

  @Test
  void deveGerarTokenValidoPeloMesmoSegredoDoProduto() {
    String token = emissor.gerar();

    NimbusJwtDecoder decoder =
        NimbusJwtDecoder.withSecretKey(
                new SecretKeySpec(
                    PROPERTIES.secret().getBytes(StandardCharsets.UTF_8), "HmacSHA256"))
            .macAlgorithm(MacAlgorithm.HS256)
            .build();
    Jwt jwt = decoder.decode(token);

    assertThat(jwt.getIssuer().toString()).isEqualTo(PROPERTIES.issuer());
    assertThat(jwt.getAudience()).containsExactly(PROPERTIES.audience());
    assertThat(jwt.getSubject()).isEqualTo("cartao-service");
    assertThat(jwt.getIssuedAt()).isEqualTo(AGORA);
    assertThat(jwt.getExpiresAt()).isEqualTo(AGORA.plusSeconds(300));
  }
}

package br.com.rpe.portador.adapters.out.http;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.rpe.portador.application.seguranca.Solicitante;
import br.com.rpe.portador.config.JwtProperties;
import com.nimbusds.jose.jwk.source.ImmutableSecret;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import javax.crypto.spec.SecretKeySpec;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

class EmissorTokenServicoTest {

  private static final byte[] SEGREDO =
      "segredo-de-teste-com-pelo-menos-32-caracteres".getBytes(StandardCharsets.UTF_8);

  @Test
  void tokenDeServicoDeveCarregarOEscopoServicoESubDoPortador() {
    var chave = new SecretKeySpec(SEGREDO, "HmacSHA256");
    var propriedades =
        new JwtProperties(
            "segredo-de-teste-com-pelo-menos-32-caracteres",
            "https://portador-service.rpe.local",
            "rpe-api",
            Duration.ofMinutes(30));
    Clock relogio = Clock.fixed(Instant.now(), ZoneOffset.UTC);
    var emissor =
        new EmissorTokenServico(
            new NimbusJwtEncoder(new ImmutableSecret<>(chave)), propriedades, relogio);

    String token = emissor.gerar();
    Jwt jwt =
        NimbusJwtDecoder.withSecretKey(chave)
            .macAlgorithm(MacAlgorithm.HS256)
            .build()
            .decode(token);

    assertThat(jwt.getSubject()).isEqualTo("portador-service");
    assertThat(jwt.getClaimAsString("scope")).isEqualTo(Solicitante.ESCOPO_SERVICO);
  }
}

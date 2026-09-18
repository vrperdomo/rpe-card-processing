package br.com.rpe.cartao.adapters.out.http;

import br.com.rpe.cartao.config.JwtProperties;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Component;

// Cartão não é emissor de tokens de usuário (só o Portador emite, ver CLAUDE.md secao 3).
// Este token é só para chamadas de serviço-para-serviço, assinado com o mesmo segredo que o
// Produto já valida como resource server.
@Component
public class EmissorTokenServico {

  private static final String SUBJECT_SERVICO = "cartao-service";
  private static final Duration EXPIRACAO = Duration.ofMinutes(5);

  private final JwtEncoder jwtEncoder;
  private final JwtProperties properties;
  private final Clock clock;

  public EmissorTokenServico(JwtEncoder jwtEncoder, JwtProperties properties, Clock clock) {
    this.jwtEncoder = jwtEncoder;
    this.properties = properties;
    this.clock = clock;
  }

  public String gerar() {
    Instant agora = clock.instant();
    JwtClaimsSet claims =
        JwtClaimsSet.builder()
            .issuer(properties.issuer())
            .audience(List.of(properties.audience()))
            .subject(SUBJECT_SERVICO)
            .issuedAt(agora)
            .expiresAt(agora.plus(EXPIRACAO))
            .build();
    JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
    return jwtEncoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
  }
}

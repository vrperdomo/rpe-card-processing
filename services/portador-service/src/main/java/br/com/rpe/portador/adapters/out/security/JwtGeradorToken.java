package br.com.rpe.portador.adapters.out.security;

import br.com.rpe.portador.application.port.out.GeradorToken;
import br.com.rpe.portador.config.JwtProperties;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Component;

@Component
public class JwtGeradorToken implements GeradorToken {

  private final JwtEncoder jwtEncoder;
  private final JwtProperties properties;
  private final Clock clock;

  public JwtGeradorToken(JwtEncoder jwtEncoder, JwtProperties properties, Clock clock) {
    this.jwtEncoder = jwtEncoder;
    this.properties = properties;
    this.clock = clock;
  }

  @Override
  public Token gerar(String subject) {
    Instant agora = clock.instant();
    Instant expiraEm = agora.plus(properties.expiracao());
    JwtClaimsSet claims =
        JwtClaimsSet.builder()
            .issuer(properties.issuer())
            .audience(List.of(properties.audience()))
            .subject(subject)
            .issuedAt(agora)
            .expiresAt(expiraEm)
            .build();
    JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
    String valor = jwtEncoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
    return new Token(valor, properties.expiracao().toSeconds());
  }
}

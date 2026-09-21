package br.com.rpe.portador.adapters.out.http;

import br.com.rpe.portador.application.seguranca.Solicitante;
import br.com.rpe.portador.config.JwtProperties;
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

// Token de serviço-para-serviço (Portador -> Produto), assinado com o mesmo segredo que o Produto
// já valida como resource server. Distinto do token de usuário emitido em /api/v1/auth/login: leva
// o escopo "servico", com o qual o Cartão dispensa a checagem de posse do recurso (ADR-009, A01).
@Component
public class EmissorTokenServico {

  private static final String SUBJECT_SERVICO = "portador-service";
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
            .claim("scope", Solicitante.ESCOPO_SERVICO)
            .issuedAt(agora)
            .expiresAt(agora.plus(EXPIRACAO))
            .build();
    JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
    return jwtEncoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
  }
}

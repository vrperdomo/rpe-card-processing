package br.com.rpe.cartao.adapters.in.web.security;

import br.com.rpe.cartao.application.seguranca.Solicitante;
import java.util.Arrays;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.oauth2.jwt.Jwt;

// Traduz o JWT já validado (assinatura, iss, aud, exp) no Solicitante da camada de aplicação
// (ADR-009, A01). Token de serviço é o que carrega o escopo "servico"; o de usuário, emitido em
// /api/v1/auth/login, nunca o carrega.
public final class SolicitanteJwt {

  private static final String CLAIM_ESCOPO = "scope";

  private SolicitanteJwt() {}

  // Sem `sub` não há como saber quem é o dono: nega (403, com log no JwtAccessDeniedHandler) em
  // vez de deixar uma IllegalArgumentException virar 500.
  public static Solicitante de(Jwt jwt) {
    String subject = jwt.getSubject();
    if (subject == null || subject.isBlank()) {
      throw new AccessDeniedException("Token sem sub");
    }
    return new Solicitante(subject, ehDeServico(jwt));
  }

  private static boolean ehDeServico(Jwt jwt) {
    String escopo = jwt.getClaimAsString(CLAIM_ESCOPO);
    return escopo != null
        && Arrays.asList(escopo.split("\\s+")).contains(Solicitante.ESCOPO_SERVICO);
  }
}

package br.com.rpe.portador.adapters.in.web.security;

import br.com.rpe.portador.adapters.in.web.ProblemDetailFactory;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.security.Principal;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

// ADR-009 (OWASP A09): todo 403 deixa um WARN com método, caminho, origem e o usuário autenticado.
// O usuário vem do `sub` de um JWT já validado (assinatura, iss e aud), então é confiável; o token
// e a query string nunca entram no log.
@Component
public class JwtAccessDeniedHandler implements AccessDeniedHandler {

  private static final Logger log = LoggerFactory.getLogger(JwtAccessDeniedHandler.class);
  private static final String USUARIO_DESCONHECIDO = "desconhecido";

  private final ProblemDetailFactory problemDetailFactory;
  private final ObjectMapper objectMapper;

  public JwtAccessDeniedHandler(
      ProblemDetailFactory problemDetailFactory, ObjectMapper objectMapper) {
    this.problemDetailFactory = problemDetailFactory;
    this.objectMapper = objectMapper;
  }

  @Override
  public void handle(
      HttpServletRequest request, HttpServletResponse response, AccessDeniedException ex)
      throws IOException {
    log.warn(
        "Acesso negado: metodo={} caminho={} origem={} usuario={}",
        request.getMethod(),
        request.getRequestURI(),
        request.getRemoteAddr(),
        usuarioAutenticado(request));
    var problem =
        problemDetailFactory.criar(
            HttpStatus.FORBIDDEN, "Sem permissão", "Você não tem permissão para este recurso");
    response.setStatus(HttpStatus.FORBIDDEN.value());
    response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
    objectMapper.writeValue(response.getOutputStream(), problem);
  }

  private static String usuarioAutenticado(HttpServletRequest request) {
    Principal principal = request.getUserPrincipal();
    return principal != null ? principal.getName() : USUARIO_DESCONHECIDO;
  }
}

package br.com.rpe.produto.adapters.in.web.security;

import br.com.rpe.produto.adapters.in.web.ProblemDetailFactory;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

// ADR-009 (OWASP A09): todo 401 deixa um WARN com método, caminho, origem e o TIPO da falha
// (token ausente, expirado, assinatura inválida...). Nunca entram no log o token, a query string
// (pode carregar dado sensível) nem a mensagem da exceção (o Spring a monta com trechos do token
// enviado pelo cliente).
@Component
public class JwtAuthEntryPoint implements AuthenticationEntryPoint {

  private static final Logger log = LoggerFactory.getLogger(JwtAuthEntryPoint.class);

  private final ProblemDetailFactory problemDetailFactory;
  private final ObjectMapper objectMapper;

  public JwtAuthEntryPoint(ProblemDetailFactory problemDetailFactory, ObjectMapper objectMapper) {
    this.problemDetailFactory = problemDetailFactory;
    this.objectMapper = objectMapper;
  }

  @Override
  public void commence(
      HttpServletRequest request, HttpServletResponse response, AuthenticationException ex)
      throws IOException {
    log.warn(
        "Requisição não autenticada: metodo={} caminho={} origem={} motivo={}",
        request.getMethod(),
        request.getRequestURI(),
        request.getRemoteAddr(),
        ex.getClass().getSimpleName());
    var problem =
        problemDetailFactory.criar(
            HttpStatus.UNAUTHORIZED, "Não autenticado", "Token ausente, inválido ou expirado");
    response.setStatus(HttpStatus.UNAUTHORIZED.value());
    response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
    objectMapper.writeValue(response.getOutputStream(), problem);
  }
}

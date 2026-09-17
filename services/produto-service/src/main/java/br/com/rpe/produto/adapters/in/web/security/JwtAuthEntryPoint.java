package br.com.rpe.produto.adapters.in.web.security;

import br.com.rpe.produto.adapters.in.web.ProblemDetailFactory;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

@Component
public class JwtAuthEntryPoint implements AuthenticationEntryPoint {

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
    var problem =
        problemDetailFactory.criar(
            HttpStatus.UNAUTHORIZED, "Não autenticado", "Token ausente, inválido ou expirado");
    response.setStatus(HttpStatus.UNAUTHORIZED.value());
    response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
    objectMapper.writeValue(response.getOutputStream(), problem);
  }
}

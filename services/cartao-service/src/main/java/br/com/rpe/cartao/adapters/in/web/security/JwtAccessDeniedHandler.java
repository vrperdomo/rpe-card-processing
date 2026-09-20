package br.com.rpe.cartao.adapters.in.web.security;

import br.com.rpe.cartao.adapters.in.web.ProblemDetailFactory;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

@Component
public class JwtAccessDeniedHandler implements AccessDeniedHandler {

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
    var problem =
        problemDetailFactory.criar(
            HttpStatus.FORBIDDEN, "Sem permissão", "Você não tem permissão para este recurso");
    response.setStatus(HttpStatus.FORBIDDEN.value());
    response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
    objectMapper.writeValue(response.getOutputStream(), problem);
  }
}

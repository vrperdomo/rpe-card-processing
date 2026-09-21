package br.com.rpe.portador.adapters.in.web;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.com.rpe.portador.adapters.in.web.security.JwtAccessDeniedHandler;
import br.com.rpe.portador.adapters.in.web.security.JwtAuthEntryPoint;
import br.com.rpe.portador.application.port.out.GeradorToken;
import br.com.rpe.portador.application.usecase.AutenticarUseCase;
import br.com.rpe.portador.config.ClockConfig;
import br.com.rpe.portador.config.SecurityConfig;
import br.com.rpe.portador.domain.exception.CredenciaisInvalidasException;
import br.com.rpe.portador.domain.exception.LimiteTentativasExcedidoException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(AuthController.class)
@Import({
  ClockConfig.class,
  SecurityConfig.class,
  ProblemDetailFactory.class,
  GlobalExceptionHandler.class,
  JwtAuthEntryPoint.class,
  JwtAccessDeniedHandler.class
})
@TestPropertySource(
    properties = {
      "rpe.jwt.secret=segredo-de-teste-com-pelo-menos-32-caracteres",
      "rpe.jwt.issuer=https://portador-service.rpe.local",
      "rpe.jwt.audience=rpe-api",
      "rpe.jwt.expiracao=30m",
      "rpe.auth.usuario-seed.username=admin",
      "rpe.auth.usuario-seed.password-hash=hash-qualquer"
    })
class AuthControllerTest {

  @Autowired private MockMvc mockMvc;
  @Autowired private ObjectMapper objectMapper;

  @MockitoBean private AutenticarUseCase autenticarUseCase;

  @Test
  void deveRetornar200ComTokenQuandoCredenciaisValidas() throws Exception {
    when(autenticarUseCase.executar("admin", "admin123", "127.0.0.1"))
        .thenReturn(new GeradorToken.Token("jwt-gerado", 1800));

    mockMvc
        .perform(
            post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(corpo("admin", "admin123")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.accessToken").value("jwt-gerado"))
        .andExpect(jsonPath("$.tokenType").value("Bearer"))
        .andExpect(jsonPath("$.expiresIn").value(1800));
  }

  @Test
  void deveRetornar401QuandoCredenciaisInvalidas() throws Exception {
    when(autenticarUseCase.executar("admin", "senha-errada", "127.0.0.1"))
        .thenThrow(new CredenciaisInvalidasException("Usuário ou senha inválidos"));

    mockMvc
        .perform(
            post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(corpo("admin", "senha-errada")))
        .andExpect(status().isUnauthorized());
  }

  @Test
  void deveRetornar429ComRetryAfterQuandoOrigemExcedeuOLimiteDeTentativas() throws Exception {
    when(autenticarUseCase.executar("admin", "admin123", "127.0.0.1"))
        .thenThrow(
            new LimiteTentativasExcedidoException("Muitas tentativas", Duration.ofSeconds(41)));

    mockMvc
        .perform(
            post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(corpo("admin", "admin123")))
        .andExpect(status().isTooManyRequests())
        .andExpect(header().string("Retry-After", "42"))
        .andExpect(jsonPath("$.title").value("Muitas tentativas"))
        .andExpect(jsonPath("$.timestamp").exists());
  }

  @Test
  void deveRetornar400QuandoUsernameEmBranco() throws Exception {
    mockMvc
        .perform(
            post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(corpo("", "admin123")))
        .andExpect(status().isBadRequest());
  }

  private String corpo(String username, String password) throws Exception {
    return objectMapper.writeValueAsString(new CorpoLogin(username, password));
  }

  private record CorpoLogin(String username, String password) {}
}

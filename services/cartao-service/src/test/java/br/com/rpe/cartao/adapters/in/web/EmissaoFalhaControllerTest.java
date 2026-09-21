package br.com.rpe.cartao.adapters.in.web;

import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.com.rpe.cartao.application.seguranca.Solicitante;
import br.com.rpe.cartao.application.usecase.BuscarFalhaEmissaoUseCase;
import br.com.rpe.cartao.config.ClockConfig;
import br.com.rpe.cartao.config.SecurityConfig;
import br.com.rpe.cartao.domain.EmissaoFalha;
import br.com.rpe.cartao.domain.exception.RecursoNaoEncontradoException;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(EmissaoFalhaController.class)
@Import({
  ClockConfig.class,
  SecurityConfig.class,
  ProblemDetailFactory.class,
  GlobalExceptionHandler.class,
  br.com.rpe.cartao.adapters.in.web.security.JwtAuthEntryPoint.class,
  br.com.rpe.cartao.adapters.in.web.security.JwtAccessDeniedHandler.class
})
@TestPropertySource(
    properties = {
      "rpe.jwt.secret=segredo-de-teste-com-pelo-menos-32-caracteres",
      "rpe.jwt.issuer=https://portador-service.rpe.local",
      "rpe.jwt.audience=rpe-api"
    })
class EmissaoFalhaControllerTest {

  private static final Instant AGORA = Instant.parse("2026-09-21T10:00:00Z");

  @Autowired private MockMvc mockMvc;

  @MockitoBean private BuscarFalhaEmissaoUseCase buscarFalhaEmissaoUseCase;

  @Test
  void deveRetornarAFalhaDoPortador() throws Exception {
    UUID portadorId = UUID.randomUUID();
    UUID produtoId = UUID.randomUUID();
    Solicitante servico = Solicitante.deServico("portador-service");
    when(buscarFalhaEmissaoUseCase.executar(portadorId, servico))
        .thenReturn(new EmissaoFalha(portadorId, produtoId, "Produto inexistente", "admin", AGORA));

    mockMvc
        .perform(
            get("/api/v1/emissao-falhas/{id}", portadorId)
                .with(
                    jwt()
                        .jwt(token -> token.subject("portador-service").claim("scope", "servico"))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.portadorId").value(portadorId.toString()))
        .andExpect(jsonPath("$.produtoId").value(produtoId.toString()))
        .andExpect(jsonPath("$.motivo").value("Produto inexistente"))
        .andExpect(jsonPath("$.ocorridaEm").value("2026-09-21T10:00:00Z"))
        .andExpect(jsonPath("$.criadoPor").doesNotExist());
  }

  @Test
  void deveRetornar404QuandoNaoHaFalhaOuNaoEDono() throws Exception {
    UUID portadorId = UUID.randomUUID();
    Solicitante admin = Solicitante.deUsuario("admin");
    when(buscarFalhaEmissaoUseCase.executar(portadorId, admin))
        .thenThrow(new RecursoNaoEncontradoException("Nenhuma falha de emissão registrada"));

    mockMvc
        .perform(
            get("/api/v1/emissao-falhas/{id}", portadorId)
                .with(jwt().jwt(token -> token.subject("admin"))))
        .andExpect(status().isNotFound());
  }

  @Test
  void deveRetornar401QuandoSemToken() throws Exception {
    mockMvc
        .perform(get("/api/v1/emissao-falhas/{id}", UUID.randomUUID()))
        .andExpect(status().isUnauthorized());
  }
}

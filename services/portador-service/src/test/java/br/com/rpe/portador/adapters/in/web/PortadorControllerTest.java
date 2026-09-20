package br.com.rpe.portador.adapters.in.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.com.rpe.portador.adapters.in.web.mapper.PortadorWebMapperImpl;
import br.com.rpe.portador.adapters.in.web.security.JwtAccessDeniedHandler;
import br.com.rpe.portador.adapters.in.web.security.JwtAuthEntryPoint;
import br.com.rpe.portador.application.usecase.CadastrarPortadorUseCase;
import br.com.rpe.portador.config.ClockConfig;
import br.com.rpe.portador.config.SecurityConfig;
import br.com.rpe.portador.domain.Cpf;
import br.com.rpe.portador.domain.Portador;
import br.com.rpe.portador.domain.exception.ConflitoException;
import br.com.rpe.portador.domain.exception.DependenciaIndisponivelException;
import br.com.rpe.portador.domain.exception.RegraNegocioException;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(PortadorController.class)
@Import({
  PortadorWebMapperImpl.class,
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
class PortadorControllerTest {

  private static final Instant AGORA = Instant.parse("2026-09-19T12:00:00Z");
  private static final String CPF_VALIDO = "529.982.247-25";
  private static final UUID PRODUTO_ID = UUID.randomUUID();

  @Autowired private MockMvc mockMvc;

  @MockitoBean private CadastrarPortadorUseCase cadastrarPortadorUseCase;

  private String corpo(String cpf, String dataNascimento, String produtoId) {
    return """
        {"nome":"Victor Rodrigues","cpf":"%s","dataNascimento":"%s","produtoId":"%s"}
        """
        .formatted(cpf, dataNascimento, produtoId);
  }

  @Test
  void deveCadastrarERetornar201ComLocationECpfMascarado() throws Exception {
    Portador portador =
        Portador.cadastrar(
            "Victor Rodrigues", Cpf.of(CPF_VALIDO), LocalDate.of(2000, 1, 1), PRODUTO_ID, AGORA);
    when(cadastrarPortadorUseCase.executar(
            "Victor Rodrigues", Cpf.of(CPF_VALIDO), LocalDate.of(2000, 1, 1), PRODUTO_ID))
        .thenReturn(portador);

    mockMvc
        .perform(
            post("/api/v1/portadores")
                .with(jwt())
                .contentType(MediaType.APPLICATION_JSON)
                .content(corpo(CPF_VALIDO, "2000-01-01", PRODUTO_ID.toString())))
        .andExpect(status().isCreated())
        .andExpect(header().string("Location", "/api/v1/portadores/" + portador.getId()))
        .andExpect(jsonPath("$.cpf").value("***.982.247-**"));
  }

  @Test
  void deveRetornar401QuandoSemToken() throws Exception {
    mockMvc
        .perform(
            post("/api/v1/portadores")
                .contentType(MediaType.APPLICATION_JSON)
                .content(corpo(CPF_VALIDO, "2000-01-01", PRODUTO_ID.toString())))
        .andExpect(status().isUnauthorized());
  }

  @Test
  void deveRetornar400QuandoCpfComFormatoInvalido() throws Exception {
    mockMvc
        .perform(
            post("/api/v1/portadores")
                .with(jwt())
                .contentType(MediaType.APPLICATION_JSON)
                .content(corpo("123", "2000-01-01", PRODUTO_ID.toString())))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.errors[0].field").value("cpf"));
  }

  @Test
  void deveRetornar422QuandoProdutoNaoEstaAtivo() throws Exception {
    when(cadastrarPortadorUseCase.executar(any(), any(), any(), any()))
        .thenThrow(new RegraNegocioException("Produto não está ATIVO"));

    mockMvc
        .perform(
            post("/api/v1/portadores")
                .with(jwt())
                .contentType(MediaType.APPLICATION_JSON)
                .content(corpo(CPF_VALIDO, "2000-01-01", PRODUTO_ID.toString())))
        .andExpect(status().isUnprocessableEntity());
  }

  @Test
  void deveRetornar422QuandoMenorDeIdade() throws Exception {
    when(cadastrarPortadorUseCase.executar(any(), any(), any(), any()))
        .thenThrow(new RegraNegocioException("Portador deve ter pelo menos 18 anos completos"));

    mockMvc
        .perform(
            post("/api/v1/portadores")
                .with(jwt())
                .contentType(MediaType.APPLICATION_JSON)
                .content(corpo(CPF_VALIDO, "2015-01-01", PRODUTO_ID.toString())))
        .andExpect(status().isUnprocessableEntity());
  }

  @Test
  void deveRetornar409QuandoCpfDuplicado() throws Exception {
    when(cadastrarPortadorUseCase.executar(any(), any(), any(), any()))
        .thenThrow(new ConflitoException("CPF já cadastrado"));

    mockMvc
        .perform(
            post("/api/v1/portadores")
                .with(jwt())
                .contentType(MediaType.APPLICATION_JSON)
                .content(corpo(CPF_VALIDO, "2000-01-01", PRODUTO_ID.toString())))
        .andExpect(status().isConflict());
  }

  @Test
  void deveRetornar409QuandoConstraintDeUnicidadeViolarNaCorrida() throws Exception {
    when(cadastrarPortadorUseCase.executar(any(), any(), any(), any()))
        .thenThrow(new DataIntegrityViolationException("uk_portador_cpf"));

    mockMvc
        .perform(
            post("/api/v1/portadores")
                .with(jwt())
                .contentType(MediaType.APPLICATION_JSON)
                .content(corpo(CPF_VALIDO, "2000-01-01", PRODUTO_ID.toString())))
        .andExpect(status().isConflict());
  }

  @Test
  void deveRetornar503ComRetryAfterQuandoProdutoIndisponivel() throws Exception {
    when(cadastrarPortadorUseCase.executar(any(), any(), any(), any()))
        .thenThrow(
            new DependenciaIndisponivelException(
                "Produto Service indisponível", Duration.ofSeconds(10)));

    mockMvc
        .perform(
            post("/api/v1/portadores")
                .with(jwt())
                .contentType(MediaType.APPLICATION_JSON)
                .content(corpo(CPF_VALIDO, "2000-01-01", PRODUTO_ID.toString())))
        .andExpect(status().isServiceUnavailable())
        .andExpect(header().string("Retry-After", "10"));
  }
}

package br.com.rpe.portador.adapters.in.web;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.com.rpe.portador.adapters.in.web.security.JwtAccessDeniedHandler;
import br.com.rpe.portador.adapters.in.web.security.JwtAuthEntryPoint;
import br.com.rpe.portador.adapters.out.security.ControleTentativasLoginEmMemoria;
import br.com.rpe.portador.application.port.out.GeradorToken;
import br.com.rpe.portador.application.port.out.VerificadorSenha;
import br.com.rpe.portador.application.usecase.AutenticarUseCase;
import br.com.rpe.portador.config.ClockConfig;
import br.com.rpe.portador.config.SecurityConfig;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

// Diferente do AuthControllerTest (caso de uso mockado), aqui o limitador é o real: prova, pela
// borda HTTP, o critério do ADR-009 - bloqueio por origem depois de N falhas, com Retry-After.
@WebMvcTest(AuthController.class)
@Import({
  ClockConfig.class,
  SecurityConfig.class,
  ProblemDetailFactory.class,
  GlobalExceptionHandler.class,
  JwtAuthEntryPoint.class,
  JwtAccessDeniedHandler.class,
  AutenticarUseCase.class,
  ControleTentativasLoginEmMemoria.class
})
@TestPropertySource(
    properties = {
      "rpe.jwt.secret=segredo-de-teste-com-pelo-menos-32-caracteres",
      "rpe.jwt.issuer=https://portador-service.rpe.local",
      "rpe.jwt.audience=rpe-api",
      "rpe.jwt.expiracao=30m",
      "rpe.auth.usuario-seed.username=admin",
      "rpe.auth.usuario-seed.password-hash=hash-qualquer",
      "rpe.auth.login-limite.max-falhas=3",
      "rpe.auth.login-limite.janela=1m"
    })
class LoginLimiteTentativasTest {

  private static final int MAX_FALHAS = 3;

  @Autowired private MockMvc mockMvc;
  @Autowired private ObjectMapper objectMapper;

  @MockitoBean private VerificadorSenha verificadorSenha;
  @MockitoBean private GeradorToken geradorToken;

  @BeforeEach
  void configurarSenhas() {
    when(verificadorSenha.confere("errada", "hash-qualquer")).thenReturn(false);
    when(verificadorSenha.confere("correta", "hash-qualquer")).thenReturn(true);
    when(geradorToken.gerar("admin")).thenReturn(new GeradorToken.Token("jwt-gerado", 1800));
  }

  @Test
  void deveRetornar429ComRetryAfterDepoisDeAtingirOLimiteDeFalhas() throws Exception {
    falhar(MAX_FALHAS, "10.1.1.1");

    mockMvc
        .perform(login("correta", "10.1.1.1"))
        .andExpect(status().isTooManyRequests())
        .andExpect(header().exists("Retry-After"))
        .andExpect(jsonPath("$.title").value("Muitas tentativas"));
  }

  @Test
  void naoDeveVerificarASenhaEnquantoAOrigemEstiverBloqueada() throws Exception {
    falhar(MAX_FALHAS, "10.1.1.2");
    mockMvc.perform(login("correta", "10.1.1.2")).andExpect(status().isTooManyRequests());

    verify(verificadorSenha, times(MAX_FALHAS)).confere(anyString(), anyString());
  }

  @Test
  void deveManterOutraOrigemLiberadaQuandoUmaOrigemEstaBloqueada() throws Exception {
    falhar(MAX_FALHAS, "10.1.1.3");

    mockMvc.perform(login("correta", "10.2.2.2")).andExpect(status().isOk());
  }

  @Test
  void naoDeveConsumirCotaComLoginsCorretos() throws Exception {
    for (int i = 0; i < MAX_FALHAS + 2; i++) {
      mockMvc.perform(login("correta", "10.1.1.4")).andExpect(status().isOk());
    }
  }

  @Test
  void deveZerarAsFalhasDepoisDeUmLoginCorreto() throws Exception {
    falhar(MAX_FALHAS - 1, "10.1.1.5");
    mockMvc.perform(login("correta", "10.1.1.5")).andExpect(status().isOk());
    falhar(MAX_FALHAS - 1, "10.1.1.5");

    mockMvc.perform(login("correta", "10.1.1.5")).andExpect(status().isOk());
  }

  private void falhar(int vezes, String origem) throws Exception {
    for (int i = 0; i < vezes; i++) {
      mockMvc.perform(login("errada", origem)).andExpect(status().isUnauthorized());
    }
  }

  private MockHttpServletRequestBuilder login(String senha, String origem) throws Exception {
    String corpo = objectMapper.writeValueAsString(new CorpoLogin("admin", senha));
    return post("/api/v1/auth/login")
        .contentType(MediaType.APPLICATION_JSON)
        .content(corpo)
        .with(origem(origem));
  }

  private static RequestPostProcessor origem(String enderecoIp) {
    return requisicao -> {
      requisicao.setRemoteAddr(enderecoIp);
      return requisicao;
    };
  }

  private record CorpoLogin(String username, String password) {}
}

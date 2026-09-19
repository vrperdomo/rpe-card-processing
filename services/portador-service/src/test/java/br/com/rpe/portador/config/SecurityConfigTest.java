package br.com.rpe.portador.config;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.com.rpe.portador.adapters.in.web.AuthController;
import br.com.rpe.portador.adapters.in.web.ProblemDetailFactory;
import br.com.rpe.portador.adapters.in.web.security.JwtAccessDeniedHandler;
import br.com.rpe.portador.adapters.in.web.security.JwtAuthEntryPoint;
import br.com.rpe.portador.application.port.out.GeradorToken;
import br.com.rpe.portador.application.usecase.AutenticarUseCase;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nimbusds.jose.JOSEObjectType;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.List;
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
class SecurityConfigTest {

  private static final String SECRET = "segredo-de-teste-com-pelo-menos-32-caracteres";
  private static final String OUTRO_SECRET = "outro-segredo-completamente-diferente-32chars";
  private static final String ISSUER = "https://portador-service.rpe.local";
  private static final String AUDIENCE = "rpe-api";

  @Autowired private MockMvc mockMvc;
  @Autowired private ObjectMapper objectMapper;

  @MockitoBean private AutenticarUseCase autenticarUseCase;

  @Test
  void deveExporLoginSemAutenticacao() throws Exception {
    when(autenticarUseCase.executar("admin", "admin123"))
        .thenReturn(new GeradorToken.Token("jwt-qualquer", 1800));

    mockMvc
        .perform(
            post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    objectMapper.writeValueAsString(new LoginRequisicaoTeste("admin", "admin123"))))
        .andExpect(status().isOk());
  }

  @Test
  void deveRetornar401ParaEndpointProtegidoSemToken() throws Exception {
    mockMvc.perform(get("/api/v1/portadores/qualquer")).andExpect(status().isUnauthorized());
  }

  @Test
  void deveDeixarPassarTokenValidoParaEndpointProtegido() throws Exception {
    String token = gerarToken(SECRET, ISSUER, List.of(AUDIENCE), Instant.now().plusSeconds(300));

    mockMvc
        .perform(get("/api/v1/portadores/qualquer").header("Authorization", "Bearer " + token))
        .andExpect(status().isNotFound());
  }

  @Test
  void deveRetornar401QuandoAssinaturaForInvalida() throws Exception {
    String token =
        gerarToken(OUTRO_SECRET, ISSUER, List.of(AUDIENCE), Instant.now().plusSeconds(300));

    mockMvc
        .perform(get("/api/v1/portadores/qualquer").header("Authorization", "Bearer " + token))
        .andExpect(status().isUnauthorized());
  }

  @Test
  void deveRetornar401QuandoTokenExpirado() throws Exception {
    String token = gerarToken(SECRET, ISSUER, List.of(AUDIENCE), Instant.now().minusSeconds(60));

    mockMvc
        .perform(get("/api/v1/portadores/qualquer").header("Authorization", "Bearer " + token))
        .andExpect(status().isUnauthorized());
  }

  @Test
  void deveRetornar401QuandoEmissorForDiferente() throws Exception {
    String token =
        gerarToken(
            SECRET,
            "https://emissor-desconhecido.local",
            List.of(AUDIENCE),
            Instant.now().plusSeconds(300));

    mockMvc
        .perform(get("/api/v1/portadores/qualquer").header("Authorization", "Bearer " + token))
        .andExpect(status().isUnauthorized());
  }

  @Test
  void deveRetornar401QuandoAudienciaForDiferente() throws Exception {
    String token =
        gerarToken(SECRET, ISSUER, List.of("outro-servico"), Instant.now().plusSeconds(300));

    mockMvc
        .perform(get("/api/v1/portadores/qualquer").header("Authorization", "Bearer " + token))
        .andExpect(status().isUnauthorized());
  }

  private static String gerarToken(
      String secret, String issuer, List<String> audience, Instant expiraEm) throws Exception {
    JWTClaimsSet claims =
        new JWTClaimsSet.Builder()
            .subject("admin")
            .issuer(issuer)
            .audience(audience)
            .issueTime(Date.from(Instant.now()))
            .expirationTime(Date.from(expiraEm))
            .build();
    SignedJWT jwt =
        new SignedJWT(
            new JWSHeader.Builder(JWSAlgorithm.HS256).type(JOSEObjectType.JWT).build(), claims);
    jwt.sign(new MACSigner(secret.getBytes(StandardCharsets.UTF_8)));
    return jwt.serialize();
  }

  private record LoginRequisicaoTeste(String username, String password) {}
}

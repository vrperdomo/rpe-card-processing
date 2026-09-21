package br.com.rpe.cartao.config;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.com.rpe.cartao.adapters.in.web.CartaoController;
import br.com.rpe.cartao.adapters.in.web.ProblemDetailFactory;
import br.com.rpe.cartao.adapters.in.web.mapper.CartaoWebMapper;
import br.com.rpe.cartao.adapters.in.web.security.JwtAccessDeniedHandler;
import br.com.rpe.cartao.adapters.in.web.security.JwtAuthEntryPoint;
import br.com.rpe.cartao.application.seguranca.Solicitante;
import br.com.rpe.cartao.application.usecase.AlterarStatusCartaoUseCase;
import br.com.rpe.cartao.application.usecase.BuscarCartaoUseCase;
import br.com.rpe.cartao.application.usecase.CartaoComProduto;
import br.com.rpe.cartao.application.usecase.ListarCartoesPorPortadorUseCase;
import br.com.rpe.cartao.domain.Cartao;
import br.com.rpe.cartao.domain.Pan;
import br.com.rpe.cartao.domain.Validade;
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
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(CartaoController.class)
@Import({
  CartaoWebMapper.class,
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
      "rpe.jwt.audience=rpe-api"
    })
class SecurityConfigTest {

  private static final Instant AGORA = Instant.parse("2026-09-20T12:00:00Z");
  private static final String SECRET = "segredo-de-teste-com-pelo-menos-32-caracteres";
  private static final String OUTRO_SECRET = "outro-segredo-completamente-diferente-32chars";
  private static final String ISSUER = "https://portador-service.rpe.local";
  private static final String AUDIENCE = "rpe-api";

  @Autowired private MockMvc mockMvc;

  @MockitoBean private BuscarCartaoUseCase buscarCartaoUseCase;
  @MockitoBean private ListarCartoesPorPortadorUseCase listarCartoesPorPortadorUseCase;
  @MockitoBean private AlterarStatusCartaoUseCase alterarStatusCartaoUseCase;

  @Test
  void deveRetornar401SemHeaderDeAutorizacao() throws Exception {
    mockMvc
        .perform(get("/api/v1/cartoes/{id}", UUID.randomUUID()))
        .andExpect(status().isUnauthorized());
  }

  @Test
  void deveAceitarTokenValido() throws Exception {
    Cartao cartao =
        Cartao.emitir(
            UUID.randomUUID(),
            UUID.randomUUID(),
            Pan.of("4532015112830366"),
            "VICTOR RODRIGUES",
            Validade.gerar(AGORA),
            "admin",
            AGORA);
    when(buscarCartaoUseCase.executar(cartao.getId(), Solicitante.deUsuario("portador-teste")))
        .thenReturn(new CartaoComProduto(cartao, Optional.empty()));

    String token = gerarToken(SECRET, ISSUER, List.of(AUDIENCE), Instant.now().plusSeconds(300));

    mockMvc
        .perform(
            get("/api/v1/cartoes/{id}", cartao.getId()).header("Authorization", "Bearer " + token))
        .andExpect(status().isOk());
  }

  @Test
  void deveRetornar401QuandoAssinaturaForInvalida() throws Exception {
    String token =
        gerarToken(OUTRO_SECRET, ISSUER, List.of(AUDIENCE), Instant.now().plusSeconds(300));

    mockMvc
        .perform(
            get("/api/v1/cartoes/{id}", UUID.randomUUID())
                .header("Authorization", "Bearer " + token))
        .andExpect(status().isUnauthorized());
  }

  @Test
  void deveRetornar401QuandoTokenExpirado() throws Exception {
    String token = gerarToken(SECRET, ISSUER, List.of(AUDIENCE), Instant.now().minusSeconds(60));

    mockMvc
        .perform(
            get("/api/v1/cartoes/{id}", UUID.randomUUID())
                .header("Authorization", "Bearer " + token))
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
        .perform(
            get("/api/v1/cartoes/{id}", UUID.randomUUID())
                .header("Authorization", "Bearer " + token))
        .andExpect(status().isUnauthorized());
  }

  @Test
  void deveRetornar401QuandoAudienciaForDiferente() throws Exception {
    String token =
        gerarToken(SECRET, ISSUER, List.of("outro-servico"), Instant.now().plusSeconds(300));

    mockMvc
        .perform(
            get("/api/v1/cartoes/{id}", UUID.randomUUID())
                .header("Authorization", "Bearer " + token))
        .andExpect(status().isUnauthorized());
  }

  private static String gerarToken(
      String secret, String issuer, List<String> audience, Instant expiraEm) throws Exception {
    JWTClaimsSet claims =
        new JWTClaimsSet.Builder()
            .subject("portador-teste")
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
}

package br.com.rpe.portador;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;

/**
 * O {@code /completo} de ponta a ponta (JWT -> use case -> cliente HTTP resiliente -> WireMock do
 * Cartão) para os quatro estados da emissão (issue #117): CONCLUIDA, PENDENTE, FALHOU e
 * DESCONHECIDA. Prova também que o caminho feliz não faz a chamada extra ao Cartão.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
class EmissaoFalhaCompletoIT extends IntegrationTestBase {

  @RegisterExtension
  static WireMockExtension wireMock =
      WireMockExtension.newInstance().options(wireMockConfig().dynamicPort()).build();

  @DynamicPropertySource
  static void propriedades(DynamicPropertyRegistry registry) {
    registry.add("rpe.portador.produto-client.base-url", wireMock::baseUrl);
    registry.add("rpe.portador.cartao-client.base-url", wireMock::baseUrl);
    registry.add("rpe.portador.outbox.relay.ativo", () -> "false");
    registry.add("resilience4j.retry.instances.cartao.wait-duration", () -> "20ms");
    // Circuito que não abre neste teste: o estado DESCONHECIDA aqui vem do 5xx, não de circuito.
    registry.add(
        "resilience4j.circuitbreaker.instances.cartao.minimum-number-of-calls", () -> "100");
  }

  @Autowired private MockMvc mockMvc;
  @Autowired private ObjectMapper objectMapper;

  @BeforeEach
  void limparStubs() {
    wireMock.resetAll();
  }

  private UUID cadastrarPortador(String cpf) throws Exception {
    UUID produtoId = UUID.randomUUID();
    wireMock.stubFor(
        get(urlEqualTo("/api/v1/produtos/" + produtoId))
            .willReturn(okJson("{\"id\":\"%s\",\"status\":\"ATIVO\"}".formatted(produtoId))));
    String resposta =
        mockMvc
            .perform(
                post("/api/v1/portadores")
                    .with(jwt().jwt(token -> token.subject("admin")))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """
                        {"nome":"Victor Rodrigues","cpf":"%s","dataNascimento":"2000-01-01","produtoId":"%s"}
                        """
                            .formatted(cpf, produtoId)))
            .andExpect(status().isCreated())
            .andReturn()
            .getResponse()
            .getContentAsString();
    return UUID.fromString(objectMapper.readTree(resposta).get("id").asText());
  }

  private void cartaoSemCartoes() {
    wireMock.stubFor(
        get(urlPathEqualTo("/api/v1/cartoes")).willReturn(okJson("{\"conteudo\":[]}")));
  }

  private org.springframework.test.web.servlet.ResultActions completo(UUID id) throws Exception {
    return mockMvc.perform(
        get("/api/v1/portadores/{id}/completo", id)
            .with(jwt().jwt(token -> token.subject("admin"))));
  }

  @Test
  void deveMostrarFalhouComMotivoQuandoOCartaoRegistrouFalha() throws Exception {
    UUID id = cadastrarPortador("52998224725");
    cartaoSemCartoes();
    wireMock.stubFor(
        get(urlEqualTo("/api/v1/emissao-falhas/" + id))
            .willReturn(
                okJson(
                    """
                    {"portadorId":"%s","produtoId":"%s","motivo":"Produto inexistente ou não ATIVO","ocorridaEm":"2026-09-21T10:00:00Z"}
                    """
                        .formatted(id, UUID.randomUUID()))));

    completo(id)
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.emissao").value("FALHOU"))
        .andExpect(jsonPath("$.cartao").doesNotExist())
        .andExpect(jsonPath("$.falhaEmissao.motivo").value("Produto inexistente ou não ATIVO"))
        .andExpect(jsonPath("$.falhaEmissao.ocorridaEm").value("2026-09-21T10:00:00Z"))
        .andExpect(jsonPath("$.avisos").isEmpty());
  }

  @Test
  void deveManterPendenteQuandoOCartaoNaoRegistrouFalha() throws Exception {
    UUID id = cadastrarPortador("11144477735");
    cartaoSemCartoes();
    wireMock.stubFor(
        get(urlEqualTo("/api/v1/emissao-falhas/" + id)).willReturn(aResponse().withStatus(404)));

    completo(id)
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.emissao").value("PENDENTE"))
        .andExpect(jsonPath("$.falhaEmissao").doesNotExist());
  }

  @Test
  void deveMostrarConcluidaSemConsultarAFalhaQuandoHaCartao() throws Exception {
    UUID id = cadastrarPortador("39053344705");
    wireMock.stubFor(
        get(urlPathEqualTo("/api/v1/cartoes"))
            .willReturn(
                okJson(
                    """
                    {"conteudo":[{"id":"%s","panMascarado":"**** **** **** 1234","validade":"09/31","status":"ATIVO"}]}
                    """
                        .formatted(UUID.randomUUID()))));

    completo(id)
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.emissao").value("CONCLUIDA"))
        .andExpect(jsonPath("$.falhaEmissao").doesNotExist());
    wireMock.verify(0, getRequestedFor(urlEqualTo("/api/v1/emissao-falhas/" + id)));
  }

  @Test
  void deveDegradarParaDesconhecidaQuandoAConsultaDaFalhaDaErro5xx() throws Exception {
    UUID id = cadastrarPortador("16899535009");
    cartaoSemCartoes();
    wireMock.stubFor(
        get(urlEqualTo("/api/v1/emissao-falhas/" + id)).willReturn(aResponse().withStatus(500)));

    completo(id)
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.emissao").value("DESCONHECIDA"))
        .andExpect(jsonPath("$.avisos[0]").value("Cartão indisponível no momento"));
  }
}

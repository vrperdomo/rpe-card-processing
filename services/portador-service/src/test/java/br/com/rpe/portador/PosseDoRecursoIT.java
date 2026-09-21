package br.com.rpe.portador;

import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.com.rpe.portador.adapters.out.persistence.OutboxEventJpaRepository;
import br.com.rpe.portador.adapters.out.persistence.PortadorJpaRepository;
import br.com.rpe.portador.domain.StatusPortador;
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
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.JwtRequestPostProcessor;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Prova de ponta a ponta (JWT -> controller -> caso de uso -> Postgres real) da posse do recurso
 * (ADR-009, A01): o dono é o {@code sub} de quem cadastrou, vai para a tabela e para o evento do
 * outbox, e quem não é o dono recebe 404 sem que nada seja lido, alterado ou consultado a jusante.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
class PosseDoRecursoIT extends IntegrationTestBase {

  @RegisterExtension
  static WireMockExtension wireMock =
      WireMockExtension.newInstance().options(wireMockConfig().dynamicPort()).build();

  @DynamicPropertySource
  static void propriedades(DynamicPropertyRegistry registry) {
    registry.add("rpe.portador.produto-client.base-url", wireMock::baseUrl);
    registry.add("rpe.portador.cartao-client.base-url", wireMock::baseUrl);
    // O relay não é o assunto deste teste e não há SQS aqui.
    registry.add("rpe.portador.outbox.relay.ativo", () -> "false");
  }

  @Autowired private MockMvc mockMvc;
  @Autowired private ObjectMapper objectMapper;
  @Autowired private PortadorJpaRepository portadorJpaRepository;
  @Autowired private OutboxEventJpaRepository outboxEventJpaRepository;

  private static JwtRequestPostProcessor usuario(String sub) {
    return jwt().jwt(token -> token.subject(sub));
  }

  @BeforeEach
  void limparStubs() {
    wireMock.resetAll();
  }

  private UUID cadastrarComo(String sub, String cpf) throws Exception {
    UUID produtoId = UUID.randomUUID();
    wireMock.stubFor(
        get(urlEqualTo("/api/v1/produtos/" + produtoId))
            .willReturn(okJson("{\"id\":\"%s\",\"status\":\"ATIVO\"}".formatted(produtoId))));
    wireMock.stubFor(
        get(urlPathEqualTo("/api/v1/cartoes")).willReturn(okJson("{\"conteudo\":[]}")));
    String corpo =
        """
        {"nome":"Victor Rodrigues","cpf":"%s","dataNascimento":"2000-01-01","produtoId":"%s"}
        """
            .formatted(cpf, produtoId);
    String resposta =
        mockMvc
            .perform(
                post("/api/v1/portadores")
                    .with(usuario(sub))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(corpo))
            .andExpect(status().isCreated())
            .andReturn()
            .getResponse()
            .getContentAsString();
    return UUID.fromString(objectMapper.readTree(resposta).get("id").asText());
  }

  @Test
  void deveGravarODonoNoPortadorENoEventoDoOutbox() throws Exception {
    UUID id = cadastrarComo("admin", "52998224725");

    assertThat(portadorJpaRepository.findById(id).orElseThrow().getCriadoPor()).isEqualTo("admin");
    String payload =
        outboxEventJpaRepository.findAll().stream()
            .filter(evento -> evento.getAggregateId().equals(id))
            .findFirst()
            .orElseThrow()
            .getPayload();
    assertThat(objectMapper.readTree(payload).at("/data/criadoPor").asText()).isEqualTo("admin");
  }

  @Test
  void donoDeveConsultarAlterarEAgregarSeuPortador() throws Exception {
    UUID id = cadastrarComo("admin", "11144477735");

    mockMvc
        .perform(get("/api/v1/portadores/{id}", id).with(usuario("admin")))
        .andExpect(status().isOk());
    mockMvc
        .perform(get("/api/v1/portadores/{id}/completo", id).with(usuario("admin")))
        .andExpect(status().isOk());
    mockMvc
        .perform(
            patch("/api/v1/portadores/{id}/status", id)
                .with(usuario("admin"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"status\":\"BLOQUEADO\"}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("BLOQUEADO"));
  }

  @Test
  void outroUsuarioDeveReceber404SemLerAlterarNemConsultarCartao() throws Exception {
    UUID id = cadastrarComo("admin", "39053344705");
    wireMock.resetRequests();

    mockMvc
        .perform(get("/api/v1/portadores/{id}", id).with(usuario("intruso")))
        .andExpect(status().isNotFound());
    mockMvc
        .perform(get("/api/v1/portadores/{id}/completo", id).with(usuario("intruso")))
        .andExpect(status().isNotFound());
    mockMvc
        .perform(
            patch("/api/v1/portadores/{id}/status", id)
                .with(usuario("intruso"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"status\":\"CANCELADO\"}"))
        .andExpect(status().isNotFound());

    assertThat(portadorJpaRepository.findById(id).orElseThrow().getStatus())
        .isEqualTo(StatusPortador.ATIVO);
    // Quem não é dono nunca dispara a consulta ao Cartão (que aceitaria o token de serviço).
    wireMock.verify(0, getRequestedFor(urlPathEqualTo("/api/v1/cartoes")));
  }

  @Test
  void naoEncontradoParaAlheioDeveSerIgualAoDeIdInexistente() throws Exception {
    UUID id = cadastrarComo("admin", "16899535009");

    String alheio =
        mockMvc
            .perform(get("/api/v1/portadores/{id}", id).with(usuario("intruso")))
            .andExpect(status().isNotFound())
            .andReturn()
            .getResponse()
            .getContentAsString();
    String inexistente =
        mockMvc
            .perform(get("/api/v1/portadores/{id}", UUID.randomUUID()).with(usuario("intruso")))
            .andExpect(status().isNotFound())
            .andReturn()
            .getResponse()
            .getContentAsString();

    assertThat(objectMapper.readTree(alheio).get("title"))
        .isEqualTo(objectMapper.readTree(inexistente).get("title"));
    assertThat(objectMapper.readTree(alheio).get("status"))
        .isEqualTo(objectMapper.readTree(inexistente).get("status"));
  }
}

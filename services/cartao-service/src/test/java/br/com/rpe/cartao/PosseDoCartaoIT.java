package br.com.rpe.cartao;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.com.rpe.cartao.adapters.out.http.ProdutoHttpClient;
import br.com.rpe.cartao.application.port.out.CartaoRepositorio;
import br.com.rpe.cartao.application.port.out.EmissaoFalhaRepositorio;
import br.com.rpe.cartao.domain.Cartao;
import br.com.rpe.cartao.domain.EmissaoFalha;
import br.com.rpe.cartao.domain.Pan;
import br.com.rpe.cartao.domain.StatusCartao;
import br.com.rpe.cartao.domain.Validade;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.MediaType;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.JwtRequestPostProcessor;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

/**
 * Prova de ponta a ponta (JWT -> controller -> caso de uso -> Postgres real) da posse do recurso
 * (ADR-009, A01): o usuário só vê, altera e lista o que criou; quem não é dono recebe 404; e o
 * token de serviço (o do Portador no {@code /completo}) continua enxergando tudo.
 */
@Testcontainers
@SpringBootTest(properties = "rpe.cartao.mensageria.listener-auto-startup=false")
@AutoConfigureMockMvc
class PosseDoCartaoIT extends IntegrationTestBase {

  private static final Instant AGORA = Instant.parse("2026-09-21T10:00:00Z");

  @Container @ServiceConnection
  static final GenericContainer<?> REDIS =
      new GenericContainer<>(DockerImageName.parse("redis:7-alpine")).withExposedPorts(6379);

  @Autowired private MockMvc mockMvc;
  @Autowired private CartaoRepositorio cartaoRepositorio;
  @Autowired private EmissaoFalhaRepositorio emissaoFalhaRepositorio;
  @MockitoBean private ProdutoHttpClient produtoHttpClient;

  private static JwtRequestPostProcessor usuario(String sub) {
    return jwt().jwt(token -> token.subject(sub));
  }

  private static JwtRequestPostProcessor servico() {
    return jwt().jwt(token -> token.subject("portador-service").claim("scope", "servico"));
  }

  private Cartao cartaoDe(UUID portadorId, String dono, String pan) {
    return cartaoRepositorio.salvar(
        Cartao.emitir(
            portadorId,
            UUID.randomUUID(),
            Pan.of(pan),
            "VICTOR RODRIGUES",
            Validade.gerar(AGORA),
            dono,
            AGORA));
  }

  @Test
  void donoDeveConsultarAlterarEListarSeuCartao() throws Exception {
    UUID portadorId = UUID.randomUUID();
    Cartao cartao = cartaoDe(portadorId, "admin", "4532015112830366");

    mockMvc
        .perform(get("/api/v1/cartoes/{id}", cartao.getId()).with(usuario("admin")))
        .andExpect(status().isOk());
    mockMvc
        .perform(
            get("/api/v1/cartoes")
                .param("portadorId", portadorId.toString())
                .with(usuario("admin")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.totalElementos").value(1));
    mockMvc
        .perform(
            patch("/api/v1/cartoes/{id}/status", cartao.getId())
                .with(usuario("admin"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"status\":\"BLOQUEADO\"}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("BLOQUEADO"));
  }

  @Test
  void outroUsuarioDeveReceber404NoGetENoPatchELista0Cartoes() throws Exception {
    UUID portadorId = UUID.randomUUID();
    Cartao cartao = cartaoDe(portadorId, "admin", "4916338506082832");

    mockMvc
        .perform(get("/api/v1/cartoes/{id}", cartao.getId()).with(usuario("intruso")))
        .andExpect(status().isNotFound());
    mockMvc
        .perform(
            patch("/api/v1/cartoes/{id}/status", cartao.getId())
                .with(usuario("intruso"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"status\":\"CANCELADO\"}"))
        .andExpect(status().isNotFound());
    mockMvc
        .perform(
            get("/api/v1/cartoes")
                .param("portadorId", portadorId.toString())
                .with(usuario("intruso")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.totalElementos").value(0))
        .andExpect(jsonPath("$.conteudo").isEmpty());

    assertThat(cartaoRepositorio.buscarPorId(cartao.getId()).orElseThrow().getStatus())
        .isEqualTo(StatusCartao.ATIVO);
  }

  @Test
  void tokenDeServicoDeveEnxergarCartoesDeQualquerDono() throws Exception {
    UUID portadorId = UUID.randomUUID();
    Cartao cartao = cartaoDe(portadorId, "admin", "4532015112830994");

    mockMvc
        .perform(get("/api/v1/cartoes/{id}", cartao.getId()).with(servico()))
        .andExpect(status().isOk());
    mockMvc
        .perform(get("/api/v1/cartoes").param("portadorId", portadorId.toString()).with(servico()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.totalElementos").value(1));
  }

  @Test
  void cartaoLegadoNaoDeveSerAcessivelAUsuarioMasSeguePeloServico() throws Exception {
    UUID portadorId = UUID.randomUUID();
    Cartao legado = cartaoDe(portadorId, "legado", "4916338506082550");

    mockMvc
        .perform(get("/api/v1/cartoes/{id}", legado.getId()).with(usuario("admin")))
        .andExpect(status().isNotFound());
    mockMvc
        .perform(get("/api/v1/cartoes/{id}", legado.getId()).with(servico()))
        .andExpect(status().isOk());
  }

  private UUID falhaDe(String dono) {
    UUID portadorId = UUID.randomUUID();
    emissaoFalhaRepositorio.registrar(
        new EmissaoFalha(
            portadorId, UUID.randomUUID(), "Produto inexistente ou não ATIVO", dono, AGORA));
    return portadorId;
  }

  @Test
  void donoEServicoDevemVerAFalhaDeEmissaoEIntrusoRecebe404() throws Exception {
    UUID portadorId = falhaDe("admin");

    mockMvc
        .perform(get("/api/v1/emissao-falhas/{id}", portadorId).with(usuario("admin")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.motivo").value("Produto inexistente ou não ATIVO"));
    mockMvc
        .perform(get("/api/v1/emissao-falhas/{id}", portadorId).with(servico()))
        .andExpect(status().isOk());
    mockMvc
        .perform(get("/api/v1/emissao-falhas/{id}", portadorId).with(usuario("intruso")))
        .andExpect(status().isNotFound());
  }

  @Test
  void deveRetornar404QuandoNaoHaFalhaRegistrada() throws Exception {
    mockMvc
        .perform(get("/api/v1/emissao-falhas/{id}", UUID.randomUUID()).with(servico()))
        .andExpect(status().isNotFound());
  }
}

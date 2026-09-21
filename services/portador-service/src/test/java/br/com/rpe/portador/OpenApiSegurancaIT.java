package br.com.rpe.portador;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.com.rpe.portador.domain.Cpf;
import br.com.rpe.portador.domain.StatusPortador;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDate;
import java.time.Period;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

/**
 * O Swagger UI só mostra o botão "Authorize" se o OpenAPI declarar o esquema de segurança. Sem ele,
 * como todo endpoint exceto o login exige JWT, o avaliador não teria como testar nada pelo Swagger
 * (o desafio cita OpenAPI/Swagger como forma de comunicação). Os exemplos dos DTOs também são
 * provados válidos: um exemplo que devolve 400 no "Try it out" é pior que nenhum.
 */
@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.MOCK,
    properties = "rpe.portador.outbox.relay.ativo=false")
@AutoConfigureMockMvc
class OpenApiSegurancaIT extends IntegrationTestBase {

  @Autowired private MockMvc mockMvc;

  private JsonNode docs() throws Exception {
    String corpo =
        mockMvc
            .perform(get("/v3/api-docs"))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();
    return new ObjectMapper().readTree(corpo);
  }

  @Test
  void deveDeclararOEsquemaBearerJwtParaOBotaoAuthorize() throws Exception {
    JsonNode esquema = docs().at("/components/securitySchemes/bearerAuth");
    assertThat(esquema.at("/type").asText()).isEqualTo("http");
    assertThat(esquema.at("/scheme").asText()).isEqualTo("bearer");
    assertThat(esquema.at("/bearerFormat").asText()).isEqualTo("JWT");
  }

  @Test
  void deveExigirOEsquemaBearerPorPadraoEmTodosOsEndpoints() throws Exception {
    assertThat(docs().at("/security/0/bearerAuth").isArray()).isTrue();
  }

  @Test
  void oLoginDeveSerOUnicoEndpointSemCadeado() throws Exception {
    JsonNode seguranca = docs().at("/paths/~1api~1v1~1auth~1login/post/security");
    assertThat(seguranca.isArray()).isTrue();
    assertThat(seguranca).isEmpty();
  }

  @Test
  void osExemplosDoPortadorDevemSerValidos() throws Exception {
    JsonNode cadastro = docs().at("/components/schemas/CadastrarPortadorRequest/properties");
    // CPF com dígitos verificadores válidos, senão o "Try it out" volta 400 (regra nossa, não do
    // PDF).
    assertThat(Cpf.of(cadastro.at("/cpf/example").asText())).isNotNull();
    LocalDate nascimento = LocalDate.parse(cadastro.at("/dataNascimento/example").asText());
    assertThat(Period.between(nascimento, LocalDate.now()).getYears()).isGreaterThanOrEqualTo(18);
    assertThat(UUID.fromString(cadastro.at("/produtoId/example").asText())).isNotNull();
    JsonNode login = docs().at("/components/schemas/LoginRequest/properties");
    assertThat(login.at("/username/example").asText()).isEqualTo("admin");
    assertThat(login.at("/password/example").asText()).isEqualTo("admin123");
    JsonNode status =
        docs().at("/components/schemas/AlterarStatusPortadorRequest/properties/status/example");
    assertThat(StatusPortador.valueOf(status.asText())).isNotNull();
  }
}

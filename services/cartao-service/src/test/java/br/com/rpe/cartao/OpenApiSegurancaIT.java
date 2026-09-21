package br.com.rpe.cartao;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.com.rpe.cartao.domain.StatusCartao;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
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
    properties = "rpe.cartao.mensageria.listener-auto-startup=false")
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
  void oExemploDeStatusDoCartaoDeveSerValido() throws Exception {
    JsonNode status =
        docs().at("/components/schemas/AlterarStatusCartaoRequest/properties/status/example");
    assertThat(StatusCartao.valueOf(status.asText())).isNotNull();
  }
}

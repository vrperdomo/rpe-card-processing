package br.com.rpe.produto;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.com.rpe.produto.domain.CategoriaProduto;
import br.com.rpe.produto.domain.StatusProduto;
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
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
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
  void osExemplosDoProdutoDevemSerValidos() throws Exception {
    JsonNode criar = docs().at("/components/schemas/CriarProdutoRequest/properties");
    assertThat(criar.at("/bin/example").asText()).matches("\\d{6}");
    assertThat(CategoriaProduto.valueOf(criar.at("/categoria/example").asText())).isNotNull();
    JsonNode status =
        docs().at("/components/schemas/AlterarStatusProdutoRequest/properties/status/example");
    assertThat(StatusProduto.valueOf(status.asText())).isNotNull();
  }
}

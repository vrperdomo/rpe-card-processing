package br.com.rpe.produto.adapters.out.messaging.dto;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.networknt.schema.JsonSchema;
import com.networknt.schema.JsonSchemaFactory;
import com.networknt.schema.SpecVersion;
import com.networknt.schema.ValidationMessage;
import java.io.InputStream;
import java.time.Instant;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.json.JsonTest;

// Contrato formal do evento publicado em produto-eventos-queue (CLAUDE.md 6.4). Serializa com o
// mesmo ObjectMapper (Spring Boot autoconfigurado) usado em producao e valida contra o schema
// canonico em docs/contracts/ (copiado para src/test/resources/contracts/ deste modulo).
@JsonTest
class ProdutoAtualizadoContratoTest {

  @Autowired private ObjectMapper objectMapper;

  @Test
  void payloadSerializadoRespeitaOContrato() throws Exception {
    ProdutoAtualizadoPayload payload =
        new ProdutoAtualizadoPayload(
            UUID.randomUUID(),
            ProdutoAtualizadoPayload.EVENT_TYPE,
            ProdutoAtualizadoPayload.EVENT_VERSION,
            Instant.now(),
            UUID.randomUUID().toString(),
            new ProdutoAtualizadoPayload.Dados(UUID.randomUUID()));

    String json = objectMapper.writeValueAsString(payload);
    JsonNode node = objectMapper.readTree(json);

    Set<ValidationMessage> violacoes = carregarSchema().validate(node);

    assertThat(violacoes).as("Violações do contrato: %s", violacoes).isEmpty();
  }

  private JsonSchema carregarSchema() throws Exception {
    JsonSchemaFactory factory = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V7);
    try (InputStream in =
        getClass().getResourceAsStream("/contracts/produto-atualizado.schema.json")) {
      return factory.getSchema(in);
    }
  }
}

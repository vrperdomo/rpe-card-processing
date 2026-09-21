package br.com.rpe.portador.application.evento;

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

// Contrato formal do evento gravado no outbox e publicado em cartao-emissao-queue pelo Relay
// (ADR-005, CLAUDE.md 6.4). Serializa com o mesmo ObjectMapper usado em producao
// (OutboxRepositorioJpaAdapter.serializar) e valida contra o schema canonico em docs/contracts/.
@JsonTest
class CartaoEmissaoSolicitadaContratoTest {

  private static final String EVENT_TYPE = "CartaoEmissaoSolicitada";

  @Autowired private ObjectMapper objectMapper;

  @Test
  void eventoDoOutboxRespeitaOContrato() throws Exception {
    CartaoEmissaoSolicitadaData data =
        new CartaoEmissaoSolicitadaData(
            UUID.randomUUID(), UUID.randomUUID(), "VICTOR RODRIGUES", "admin");
    EventoOutbox evento =
        EventoOutbox.criar(EVENT_TYPE, UUID.randomUUID().toString(), data, Instant.now());

    String json = objectMapper.writeValueAsString(evento);
    JsonNode node = objectMapper.readTree(json);

    Set<ValidationMessage> violacoes = carregarSchema().validate(node);

    assertThat(violacoes).as("Violações do contrato: %s", violacoes).isEmpty();
    // Opcional no schema (eventos antigos), mas o Portador sempre o publica (ADR-009, A01).
    assertThat(node.at("/data/criadoPor").asText()).isEqualTo("admin");
  }

  private JsonSchema carregarSchema() throws Exception {
    JsonSchemaFactory factory = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V7);
    try (InputStream in =
        getClass().getResourceAsStream("/contracts/cartao-emissao-solicitada.schema.json")) {
      return factory.getSchema(in);
    }
  }
}

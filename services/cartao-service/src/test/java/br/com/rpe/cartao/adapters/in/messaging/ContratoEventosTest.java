package br.com.rpe.cartao.adapters.in.messaging;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.networknt.schema.JsonSchema;
import com.networknt.schema.JsonSchemaFactory;
import com.networknt.schema.SpecVersion;
import com.networknt.schema.ValidationMessage;
import java.io.InputStream;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.json.JsonTest;

// Fecha o contrato dos dois lados: uma amostra realista do payload que os produtores (produto-
// service e portador-service) de fato emitem (mesmo formato de envelope) precisa (1) respeitar o
// schema formal e (2) desserializar corretamente nos records que o Cartao usa para consumir
// (CLAUDE.md 6.4). Se um dos dois lados divergir do contrato, este teste falha antes de produção.
@JsonTest
class ContratoEventosTest {

  @Autowired private ObjectMapper objectMapper;

  @Test
  void amostraDeProdutoAtualizadoRespeitaOContratoEDesserializaCorretamente() throws Exception {
    UUID eventId = UUID.randomUUID();
    UUID produtoId = UUID.randomUUID();
    String json =
        """
        {
          "eventId": "%s",
          "eventType": "ProdutoAtualizado",
          "eventVersion": 1,
          "occurredAt": "2026-09-20T21:00:00Z",
          "correlationId": "%s",
          "data": { "produtoId": "%s" }
        }
        """
            .formatted(eventId, UUID.randomUUID(), produtoId);

    validarContra("/contracts/produto-atualizado.schema.json", json);

    ProdutoAtualizadoMensagem mensagem =
        objectMapper.readValue(json, ProdutoAtualizadoMensagem.class);
    assertThat(mensagem.valida()).isTrue();
    assertThat(mensagem.data().produtoId()).isEqualTo(produtoId);
  }

  @Test
  void amostraDeCartaoEmissaoSolicitadaRespeitaOContratoEDesserializaCorretamente()
      throws Exception {
    UUID eventId = UUID.randomUUID();
    UUID portadorId = UUID.randomUUID();
    UUID produtoId = UUID.randomUUID();
    String json =
        """
        {
          "eventId": "%s",
          "eventType": "CartaoEmissaoSolicitada",
          "eventVersion": 1,
          "occurredAt": "2026-09-20T21:00:00Z",
          "correlationId": "%s",
          "data": {
            "portadorId": "%s",
            "produtoId": "%s",
            "nomeImpresso": "VICTOR RODRIGUES"
          }
        }
        """
            .formatted(eventId, UUID.randomUUID(), portadorId, produtoId);

    validarContra("/contracts/cartao-emissao-solicitada.schema.json", json);

    CartaoEmissaoSolicitadaMensagem mensagem =
        objectMapper.readValue(json, CartaoEmissaoSolicitadaMensagem.class);
    assertThat(mensagem.valida()).isTrue();
    assertThat(mensagem.eventId()).isEqualTo(eventId);
    assertThat(mensagem.data().portadorId()).isEqualTo(portadorId);
    assertThat(mensagem.data().produtoId()).isEqualTo(produtoId);
  }

  private void validarContra(String caminhoSchema, String json) throws Exception {
    JsonNode node = objectMapper.readTree(json);
    JsonSchemaFactory factory = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V7);
    try (InputStream in = getClass().getResourceAsStream(caminhoSchema)) {
      JsonSchema schema = factory.getSchema(in);
      Set<ValidationMessage> violacoes = schema.validate(node);
      assertThat(violacoes).as("Violações do contrato: %s", violacoes).isEmpty();
    }
  }
}

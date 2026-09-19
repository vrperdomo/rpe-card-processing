package br.com.rpe.cartao.adapters.in.messaging;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.UUID;

// Espelha o contrato publicado pelo Produto Service (PRD 8.3 / decisao 19.3), ignorando campos
// que o Cartao nao usa (eventId, occurredAt, correlationId ja chega via atributo SQS).
@JsonIgnoreProperties(ignoreUnknown = true)
public record ProdutoAtualizadoMensagem(String eventType, int eventVersion, Dados data) {

  public static final String EVENT_TYPE = "ProdutoAtualizado";
  public static final int EVENT_VERSION_SUPORTADA = 1;

  @JsonIgnoreProperties(ignoreUnknown = true)
  public record Dados(UUID produtoId) {}

  public boolean valida() {
    return EVENT_TYPE.equals(eventType)
        && eventVersion == EVENT_VERSION_SUPORTADA
        && data != null
        && data.produtoId() != null;
  }
}

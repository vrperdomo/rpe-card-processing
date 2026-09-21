package br.com.rpe.cartao.adapters.in.messaging;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.UUID;

// Espelha o contrato publicado pelo Portador via Outbox Relay (PRD 8.3), ignorando campos que o
// Cartao nao usa (eventId do envelope fica fora do record de proposito - ver Listener - occurredAt,
// correlationId ja chega via atributo SQS).
@JsonIgnoreProperties(ignoreUnknown = true)
public record CartaoEmissaoSolicitadaMensagem(
    UUID eventId, String eventType, int eventVersion, Dados data) {

  public static final String EVENT_TYPE = "CartaoEmissaoSolicitada";
  public static final int EVENT_VERSION_SUPORTADA = 1;

  @JsonIgnoreProperties(ignoreUnknown = true)
  // criadoPor é opcional: eventos anteriores ao #121 não o trazem (ver EmitirCartaoUseCase).
  public record Dados(UUID portadorId, UUID produtoId, String nomeImpresso, String criadoPor) {}

  public boolean valida() {
    return eventId != null
        && EVENT_TYPE.equals(eventType)
        && eventVersion == EVENT_VERSION_SUPORTADA
        && data != null
        && data.portadorId() != null
        && data.produtoId() != null
        && data.nomeImpresso() != null
        && !data.nomeImpresso().isBlank();
  }
}

package br.com.rpe.portador.adapters.out.messaging;

import br.com.rpe.portador.application.evento.EventoPendente;
import br.com.rpe.portador.application.port.out.PublicadorEventos;
import br.com.rpe.portador.config.OutboxRelayProperties;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.awspring.cloud.sqs.operations.SqsTemplate;
import java.util.HashMap;
import java.util.Map;
import org.springframework.stereotype.Component;

// Atributos SQS (correlationId, eventType, eventVersion) conforme PRD 8.3 — extraidos do proprio
// payload, ja que o outbox_event nao tem colunas dedicadas para eles (o payload e a fonte unica).
@Component
public class SqsPublicadorEventos implements PublicadorEventos {

  private final SqsTemplate sqsTemplate;
  private final OutboxRelayProperties properties;
  private final ObjectMapper objectMapper;

  public SqsPublicadorEventos(
      SqsTemplate sqsTemplate, OutboxRelayProperties properties, ObjectMapper objectMapper) {
    this.sqsTemplate = sqsTemplate;
    this.properties = properties;
    this.objectMapper = objectMapper;
  }

  @Override
  public void publicar(EventoPendente evento) {
    Map<String, Object> atributos = extrairAtributosSqs(evento.payload());
    sqsTemplate.send(
        to -> to.queue(properties.fila()).payload(evento.payload()).headers(atributos));
  }

  private Map<String, Object> extrairAtributosSqs(String payload) {
    Map<String, Object> atributos = new HashMap<>();
    try {
      JsonNode json = objectMapper.readTree(payload);
      atributos.put("correlationId", textoOuVazio(json, "correlationId"));
      atributos.put("eventType", textoOuVazio(json, "eventType"));
      atributos.put("eventVersion", String.valueOf(json.path("eventVersion").asInt()));
    } catch (Exception ex) {
      throw new IllegalStateException("Payload de outbox inválido, não foi possível publicar", ex);
    }
    return atributos;
  }

  private String textoOuVazio(JsonNode json, String campo) {
    return json.path(campo).asText("");
  }
}

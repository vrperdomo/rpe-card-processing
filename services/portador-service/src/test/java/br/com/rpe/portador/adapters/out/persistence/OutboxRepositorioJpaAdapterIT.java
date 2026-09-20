package br.com.rpe.portador.adapters.out.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.rpe.portador.IntegrationTestBase;
import br.com.rpe.portador.application.evento.CartaoEmissaoSolicitadaData;
import br.com.rpe.portador.application.evento.EventoOutbox;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.jackson.JacksonAutoConfiguration;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({JacksonAutoConfiguration.class, OutboxRepositorioJpaAdapter.class})
class OutboxRepositorioJpaAdapterIT extends IntegrationTestBase {

  private static final Instant AGORA = Instant.parse("2026-09-19T12:00:00Z");

  @Autowired private OutboxRepositorioJpaAdapter adapter;
  @Autowired private OutboxEventJpaRepository jpaRepository;
  @Autowired private TestEntityManager entityManager;
  @Autowired private ObjectMapper objectMapper;

  @Test
  void deveRegistrarEventoComStatusPendenteETentativasZero() throws Exception {
    UUID portadorId = UUID.randomUUID();
    UUID produtoId = UUID.randomUUID();
    EventoOutbox evento =
        EventoOutbox.criar(
            "CartaoEmissaoSolicitada",
            "corr-abc",
            new CartaoEmissaoSolicitadaData(portadorId, produtoId, "VICTOR"),
            AGORA);

    adapter.registrar(portadorId, "Portador", evento);
    entityManager.flush();
    entityManager.clear();

    OutboxEventEntity persistido = jpaRepository.findById(evento.eventId()).orElseThrow();
    assertThat(persistido.getAggregateType()).isEqualTo("Portador");
    assertThat(persistido.getAggregateId()).isEqualTo(portadorId);
    assertThat(persistido.getEventType()).isEqualTo("CartaoEmissaoSolicitada");
    assertThat(persistido.getStatus()).isEqualTo(StatusOutboxEvent.PENDENTE);
    assertThat(persistido.getTentativas()).isZero();
    assertThat(persistido.getCriadoEm()).isEqualTo(AGORA);
    assertThat(persistido.getPublicadoEm()).isNull();

    var payload = objectMapper.readTree(persistido.getPayload());
    assertThat(payload.get("eventType").asText()).isEqualTo("CartaoEmissaoSolicitada");
    assertThat(payload.get("correlationId").asText()).isEqualTo("corr-abc");
    assertThat(payload.get("data").get("nomeImpresso").asText()).isEqualTo("VICTOR");
    assertThat(payload.has("cpf")).isFalse();
  }
}

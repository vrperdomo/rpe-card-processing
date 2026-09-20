package br.com.rpe.portador.adapters.out.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.rpe.portador.IntegrationTestBase;
import br.com.rpe.portador.application.evento.CartaoEmissaoSolicitadaData;
import br.com.rpe.portador.application.evento.EventoOutbox;
import br.com.rpe.portador.application.evento.EventoPendente;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.jackson.JacksonAutoConfiguration;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;

// Clock fixo em AGORA (nao ClockConfig real) de proposito: buscarLotePendente compara
// proxima_tentativa_em contra Instant.now(clock), entao o teste precisa controlar "agora" para que
// "tentativa futura" seja deterministicamente futura, independente da data real da maquina.
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({
  JacksonAutoConfiguration.class,
  OutboxRepositorioJpaAdapterIT.TestConfig.class,
  OutboxRepositorioJpaAdapter.class
})
class OutboxRepositorioJpaAdapterIT extends IntegrationTestBase {

  private static final Instant AGORA = Instant.parse("2026-09-19T12:00:00Z");

  @Autowired private OutboxRepositorioJpaAdapter adapter;
  @Autowired private OutboxEventJpaRepository jpaRepository;
  @Autowired private TestEntityManager entityManager;
  @Autowired private ObjectMapper objectMapper;
  @Autowired private SimpleMeterRegistry meterRegistry;

  static class TestConfig {
    @Bean
    SimpleMeterRegistry meterRegistry() {
      return new SimpleMeterRegistry();
    }

    @Bean
    Clock clock() {
      return Clock.fixed(AGORA, ZoneOffset.UTC);
    }
  }

  private EventoOutbox eventoQualquer(UUID aggregateId) {
    return EventoOutbox.criar(
        "CartaoEmissaoSolicitada",
        "corr-abc",
        new CartaoEmissaoSolicitadaData(aggregateId, UUID.randomUUID(), "VICTOR"),
        AGORA);
  }

  @Test
  void deveRegistrarEventoComStatusPendenteETentativasZero() throws Exception {
    UUID portadorId = UUID.randomUUID();
    EventoOutbox evento = eventoQualquer(portadorId);

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

  @Test
  void buscarLotePendenteDeveIgnorarEventosJaPublicadosOuComTentativaFutura() {
    UUID idAindaPendente = registrarERecuperarId();
    UUID idPublicado = registrarERecuperarId();
    UUID idComTentativaFutura = registrarERecuperarId();
    entityManager.flush();

    adapter.marcarPublicado(idPublicado, AGORA);
    adapter.marcarFalhaTemporaria(idComTentativaFutura, AGORA.plus(Duration.ofDays(1)), "erro");
    entityManager.flush();
    entityManager.clear();

    List<EventoPendente> lote = adapter.buscarLotePendente(10);

    assertThat(lote).extracting(EventoPendente::id).contains(idAindaPendente);
    assertThat(lote)
        .extracting(EventoPendente::id)
        .doesNotContain(idPublicado, idComTentativaFutura);
  }

  @Test
  void buscarLotePendenteDeveRespeitarLimiteDoLote() {
    for (int i = 0; i < 5; i++) {
      adapter.registrar(UUID.randomUUID(), "Portador", eventoQualquer(UUID.randomUUID()));
    }
    entityManager.flush();
    entityManager.clear();

    List<EventoPendente> lote = adapter.buscarLotePendente(3);

    assertThat(lote).hasSize(3);
  }

  @Test
  void marcarPublicadoDeveAtualizarStatusEData() {
    UUID id = registrarERecuperarId();
    entityManager.flush();
    entityManager.clear();

    adapter.marcarPublicado(id, AGORA);
    entityManager.flush();
    entityManager.clear();

    OutboxEventEntity persistido = jpaRepository.findById(id).orElseThrow();
    assertThat(persistido.getStatus()).isEqualTo(StatusOutboxEvent.PUBLICADO);
    assertThat(persistido.getPublicadoEm()).isEqualTo(AGORA);
  }

  @Test
  void marcarFalhaTemporariaDeveIncrementarTentativasESeguirPendente() {
    UUID id = registrarERecuperarId();
    entityManager.flush();
    entityManager.clear();

    adapter.marcarFalhaTemporaria(id, AGORA.plusSeconds(10), "SQS indisponível");
    entityManager.flush();
    entityManager.clear();

    OutboxEventEntity persistido = jpaRepository.findById(id).orElseThrow();
    assertThat(persistido.getStatus()).isEqualTo(StatusOutboxEvent.PENDENTE);
    assertThat(persistido.getTentativas()).isEqualTo(1);
    assertThat(persistido.getProximaTentativaEm()).isEqualTo(AGORA.plusSeconds(10));
    assertThat(persistido.getUltimoErro()).isEqualTo("SQS indisponível");
  }

  @Test
  void marcarFalhaDefinitivaDeveMarcarFalhouEIncrementarMetrica() {
    UUID id = registrarERecuperarId();
    entityManager.flush();
    entityManager.clear();
    double falhasAntes = contarMetricaFalhas();

    adapter.marcarFalhaDefinitiva(id, "produto inexistente");
    entityManager.flush();
    entityManager.clear();

    OutboxEventEntity persistido = jpaRepository.findById(id).orElseThrow();
    assertThat(persistido.getStatus()).isEqualTo(StatusOutboxEvent.FALHOU);
    assertThat(persistido.getTentativas()).isEqualTo(1);
    assertThat(contarMetricaFalhas()).isEqualTo(falhasAntes + 1);
  }

  @Test
  void deveExporGaugeDeEventosPendentes() {
    adapter.registrar(UUID.randomUUID(), "Portador", eventoQualquer(UUID.randomUUID()));
    entityManager.flush();

    Double valor = meterRegistry.get("outbox.pendentes").gauge().value();

    assertThat(valor).isGreaterThanOrEqualTo(1.0);
  }

  private double contarMetricaFalhas() {
    var counter = meterRegistry.find("outbox.falhas").counter();
    return counter == null ? 0.0 : counter.count();
  }

  private UUID registrarERecuperarId() {
    UUID aggregateId = UUID.randomUUID();
    EventoOutbox evento = eventoQualquer(aggregateId);
    adapter.registrar(aggregateId, "Portador", evento);
    return evento.eventId();
  }
}

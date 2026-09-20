package br.com.rpe.portador.application.usecase;

import br.com.rpe.portador.application.evento.EventoPendente;
import br.com.rpe.portador.application.port.out.OutboxRepositorio;
import br.com.rpe.portador.application.port.out.PublicadorEventos;
import br.com.rpe.portador.config.OutboxRelayProperties;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Le um lote de outbox_event pendentes (SELECT ... FOR UPDATE SKIP LOCKED, seguro com multiplas
 * replicas do Portador rodando o relay ao mesmo tempo) e publica no SQS. Falha de publicacao nunca
 * e classificada como transitoria/definitiva (diferente do consumer): aqui toda falha so significa
 * "tentar de novo mais tarde", ate esgotar max-tentativas (PRD 9.1).
 *
 * <p>Disparo agendado fica no bean separado {@link OutboxRelayScheduler} de proposito: o
 * agendamento verifica {@code properties.ativo()} antes de chamar este metodo, e uma unica
 * anotacao @Transactional aqui abriria a conexao do banco mesmo com o relay desativado.
 */
@Service
public class OutboxRelayUseCase {

  private static final Logger log = LoggerFactory.getLogger(OutboxRelayUseCase.class);

  private final OutboxRepositorio outboxRepositorio;
  private final PublicadorEventos publicadorEventos;
  private final OutboxRelayProperties properties;
  private final Clock clock;

  public OutboxRelayUseCase(
      OutboxRepositorio outboxRepositorio,
      PublicadorEventos publicadorEventos,
      OutboxRelayProperties properties,
      Clock clock) {
    this.outboxRepositorio = outboxRepositorio;
    this.publicadorEventos = publicadorEventos;
    this.properties = properties;
    this.clock = clock;
  }

  @Transactional
  public void executar() {
    List<EventoPendente> lote = outboxRepositorio.buscarLotePendente(properties.lote());
    for (EventoPendente evento : lote) {
      processar(evento);
    }
  }

  private void processar(EventoPendente evento) {
    try {
      publicadorEventos.publicar(evento);
      outboxRepositorio.marcarPublicado(evento.id(), Instant.now(clock));
    } catch (RuntimeException ex) {
      log.warn("Falha ao publicar evento de outbox {}", evento.id(), ex);
      registrarFalha(evento, ex);
    }
  }

  private void registrarFalha(EventoPendente evento, RuntimeException ex) {
    int tentativasApos = evento.tentativas() + 1;
    String motivo = ex.getMessage();
    if (tentativasApos >= properties.maxTentativas()) {
      outboxRepositorio.marcarFalhaDefinitiva(evento.id(), motivo);
    } else {
      outboxRepositorio.marcarFalhaTemporaria(
          evento.id(), calcularProximaTentativa(tentativasApos), motivo);
    }
  }

  private Instant calcularProximaTentativa(int tentativasApos) {
    long baseMillis = properties.backoffBase().toMillis();
    long exponencial = baseMillis * (1L << (tentativasApos - 1));
    long jitter = ThreadLocalRandom.current().nextLong(exponencial / 2 + 1);
    return Instant.now(clock).plusMillis(exponencial + jitter);
  }
}

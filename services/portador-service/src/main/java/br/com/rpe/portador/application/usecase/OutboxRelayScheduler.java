package br.com.rpe.portador.application.usecase;

import br.com.rpe.portador.config.OutboxRelayProperties;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Bean separado de {@link OutboxRelayUseCase} de propósito: sem @Transactional aqui, desligar o
 * relay (properties.ativo() = false) nunca abre uma conexão com o banco, só decide se chama o caso
 * de uso.
 */
@Component
public class OutboxRelayScheduler {

  private final OutboxRelayUseCase outboxRelayUseCase;
  private final OutboxRelayProperties properties;

  public OutboxRelayScheduler(
      OutboxRelayUseCase outboxRelayUseCase, OutboxRelayProperties properties) {
    this.outboxRelayUseCase = outboxRelayUseCase;
    this.properties = properties;
  }

  @Scheduled(fixedDelayString = "${rpe.portador.outbox.relay.intervalo}")
  public void executar() {
    if (properties.ativo()) {
      outboxRelayUseCase.executar();
    }
  }
}

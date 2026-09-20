package br.com.rpe.portador.application.usecase;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import br.com.rpe.portador.config.OutboxRelayProperties;
import java.time.Duration;
import org.junit.jupiter.api.Test;

class OutboxRelaySchedulerTest {

  private final OutboxRelayUseCase outboxRelayUseCase = mock(OutboxRelayUseCase.class);

  private OutboxRelayScheduler scheduler(boolean ativo) {
    OutboxRelayProperties properties =
        new OutboxRelayProperties(
            "cartao-emissao-queue", 20, Duration.ofSeconds(5), 3, Duration.ofSeconds(2), ativo);
    return new OutboxRelayScheduler(outboxRelayUseCase, properties);
  }

  @Test
  void deveChamarOCasoDeUsoQuandoAtivo() {
    scheduler(true).executar();

    verify(outboxRelayUseCase).executar();
  }

  @Test
  void naoDeveChamarOCasoDeUsoQuandoDesativado() {
    scheduler(false).executar();

    verify(outboxRelayUseCase, never()).executar();
  }
}

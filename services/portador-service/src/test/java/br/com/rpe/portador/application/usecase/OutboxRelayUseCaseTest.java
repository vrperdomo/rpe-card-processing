package br.com.rpe.portador.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import br.com.rpe.portador.application.evento.EventoPendente;
import br.com.rpe.portador.application.port.out.OutboxRepositorio;
import br.com.rpe.portador.application.port.out.PublicadorEventos;
import br.com.rpe.portador.config.OutboxRelayProperties;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class OutboxRelayUseCaseTest {

  private static final Instant AGORA = Instant.parse("2026-09-20T12:00:00Z");

  private final OutboxRepositorio outboxRepositorio = mock(OutboxRepositorio.class);
  private final PublicadorEventos publicadorEventos = mock(PublicadorEventos.class);
  private final Clock clock = Clock.fixed(AGORA, ZoneOffset.UTC);
  private final OutboxRelayProperties properties =
      new OutboxRelayProperties(
          "cartao-emissao-queue", 20, Duration.ofSeconds(5), 3, Duration.ofSeconds(2), true);
  private final OutboxRelayUseCase useCase =
      new OutboxRelayUseCase(outboxRepositorio, publicadorEventos, properties, clock);

  private EventoPendente evento(int tentativas) {
    return new EventoPendente(
        UUID.randomUUID(), UUID.randomUUID(), "CartaoEmissaoSolicitada", "{}", tentativas);
  }

  @Test
  void deveMarcarPublicadoQuandoEnvioTemSucesso() {
    EventoPendente evento = evento(0);
    when(outboxRepositorio.buscarLotePendente(20)).thenReturn(List.of(evento));

    useCase.executar();

    verify(publicadorEventos).publicar(evento);
    verify(outboxRepositorio).marcarPublicado(evento.id(), AGORA);
    verify(outboxRepositorio, never()).marcarFalhaTemporaria(any(), any(), any());
    verify(outboxRepositorio, never()).marcarFalhaDefinitiva(any(), any());
  }

  @Test
  void deveMarcarFalhaTemporariaComBackoffQuandoAindaHaTentativasDisponiveis() {
    EventoPendente evento = evento(0);
    when(outboxRepositorio.buscarLotePendente(20)).thenReturn(List.of(evento));
    doThrow(new RuntimeException("SQS indisponível")).when(publicadorEventos).publicar(evento);

    useCase.executar();

    ArgumentCaptor<Instant> proximaTentativaCaptor = ArgumentCaptor.forClass(Instant.class);
    verify(outboxRepositorio)
        .marcarFalhaTemporaria(eq(evento.id()), proximaTentativaCaptor.capture(), any());
    verify(outboxRepositorio, never()).marcarFalhaDefinitiva(any(), any());
    // backoff = base(2s) * 2^0 = 2s, +ate 50% de jitter
    Instant proximaTentativa = proximaTentativaCaptor.getValue();
    assertThat(proximaTentativa).isAfterOrEqualTo(AGORA.plusSeconds(2));
    assertThat(proximaTentativa).isBeforeOrEqualTo(AGORA.plusSeconds(3));
  }

  @Test
  void deveAumentarBackoffExponencialmenteConformeTentativasAnteriores() {
    EventoPendente evento = evento(1); // 1 tentativa anterior, esta sera a 2a (ainda < max=3)
    when(outboxRepositorio.buscarLotePendente(20)).thenReturn(List.of(evento));
    doThrow(new RuntimeException("SQS indisponível")).when(publicadorEventos).publicar(evento);

    useCase.executar();

    ArgumentCaptor<Instant> proximaTentativaCaptor = ArgumentCaptor.forClass(Instant.class);
    verify(outboxRepositorio)
        .marcarFalhaTemporaria(eq(evento.id()), proximaTentativaCaptor.capture(), any());
    // backoff = base(2s) * 2^1 = 4s, +ate 50% de jitter
    Instant proximaTentativa = proximaTentativaCaptor.getValue();
    assertThat(proximaTentativa).isAfterOrEqualTo(AGORA.plusSeconds(4));
    assertThat(proximaTentativa).isBeforeOrEqualTo(AGORA.plusSeconds(6));
  }

  @Test
  void deveMarcarFalhaDefinitivaQuandoAtingirMaximoDeTentativas() {
    EventoPendente evento = evento(2); // maxTentativas = 3, esta e a 3a tentativa
    when(outboxRepositorio.buscarLotePendente(20)).thenReturn(List.of(evento));
    doThrow(new RuntimeException("SQS indisponível")).when(publicadorEventos).publicar(evento);

    useCase.executar();

    verify(outboxRepositorio).marcarFalhaDefinitiva(eq(evento.id()), any());
    verify(outboxRepositorio, never()).marcarFalhaTemporaria(any(), any(), any());
  }

  @Test
  void deveContinuarProcessandoLoteMesmoQuandoUmEventoFalha() {
    EventoPendente falhou = evento(0);
    EventoPendente sucesso = evento(0);
    when(outboxRepositorio.buscarLotePendente(20)).thenReturn(List.of(falhou, sucesso));
    doThrow(new RuntimeException("erro")).when(publicadorEventos).publicar(falhou);

    useCase.executar();

    verify(publicadorEventos, times(1)).publicar(sucesso);
    verify(outboxRepositorio).marcarPublicado(sucesso.id(), AGORA);
    verify(outboxRepositorio).marcarFalhaTemporaria(eq(falhou.id()), any(), any());
  }

  @Test
  void naoDeveFazerNadaQuandoNaoHaEventosPendentes() {
    when(outboxRepositorio.buscarLotePendente(20)).thenReturn(List.of());

    useCase.executar();

    verify(publicadorEventos, never()).publicar(any());
  }
}

package br.com.rpe.portador.adapters.out.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.com.rpe.portador.config.LoginLimiteProperties;
import br.com.rpe.portador.domain.exception.LimiteTentativasExcedidoException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

class ControleTentativasLoginEmMemoriaTest {

  private static final String ORIGEM = "10.0.0.1";
  private static final Duration JANELA = Duration.ofMinutes(1);

  private final RelogioMutavel relogio = new RelogioMutavel(Instant.parse("2026-09-21T10:00:00Z"));
  private final ControleTentativasLoginEmMemoria controle =
      new ControleTentativasLoginEmMemoria(new LoginLimiteProperties(3, JANELA, 100), relogio);

  @Test
  void devePermitirTentativasEnquantoNaoAtingemOLimite() {
    assertThatCode(() -> tentar(ORIGEM, 3)).doesNotThrowAnyException();
  }

  @Test
  void deveBloquearATentativaSeguinteAoLimite() {
    tentar(ORIGEM, 3);

    assertThatThrownBy(() -> controle.registrarTentativa(ORIGEM))
        .isInstanceOf(LimiteTentativasExcedidoException.class);
  }

  @Test
  void deveInformarRetryAfterComOTempoRestanteDaJanelaAPartirDaTentativaMaisAntiga() {
    controle.registrarTentativa(ORIGEM);
    relogio.avancar(Duration.ofSeconds(20));
    tentar(ORIGEM, 2);

    assertThatThrownBy(() -> controle.registrarTentativa(ORIGEM))
        .isInstanceOfSatisfying(
            LimiteTentativasExcedidoException.class,
            ex -> assertThat(ex.getRetryAfter()).isEqualTo(Duration.ofSeconds(40)));
  }

  @Test
  void deveLiberarQuandoAJanelaExpira() {
    tentar(ORIGEM, 3);

    relogio.avancar(JANELA);

    assertThatCode(() -> controle.registrarTentativa(ORIGEM)).doesNotThrowAnyException();
  }

  @Test
  void naoDeveContarTentativasForaDaJanela() {
    tentar(ORIGEM, 2);
    relogio.avancar(JANELA.plusSeconds(1));

    assertThatCode(() -> tentar(ORIGEM, 3)).doesNotThrowAnyException();
  }

  @Test
  void deveZerarAsTentativasQuandoLoginTemSucesso() {
    tentar(ORIGEM, 3);

    controle.registrarSucesso(ORIGEM);

    assertThatCode(() -> tentar(ORIGEM, 3)).doesNotThrowAnyException();
  }

  @Test
  void deveManterOrigensIndependentes() {
    tentar(ORIGEM, 3);

    assertThatCode(() -> controle.registrarTentativa("10.0.0.2")).doesNotThrowAnyException();
  }

  @Test
  void deveDescartarAOrigemMenosRecenteQuandoAtingeOTetoDeOrigensRastreadas() {
    ControleTentativasLoginEmMemoria limitado =
        new ControleTentativasLoginEmMemoria(new LoginLimiteProperties(1, JANELA, 2), relogio);
    limitado.registrarTentativa("a");
    limitado.registrarTentativa("b");
    limitado.registrarTentativa("c");

    assertThatCode(() -> limitado.registrarTentativa("a")).doesNotThrowAnyException();
    assertThatThrownBy(() -> limitado.registrarTentativa("c"))
        .isInstanceOf(LimiteTentativasExcedidoException.class);
  }

  // Reproduz a rajada paralela de um atacante: se checar o limite e reservar a tentativa fossem
  // passos separados, várias threads passariam pela checagem antes de qualquer reserva.
  @Test
  void deveAceitarNoMaximoOLimiteDeTentativasMesmoComRequisicoesConcorrentes() throws Exception {
    int requisicoes = 50;
    AtomicInteger aceitas = new AtomicInteger();
    AtomicInteger bloqueadas = new AtomicInteger();
    CountDownLatch largada = new CountDownLatch(1);
    ExecutorService executor = Executors.newFixedThreadPool(requisicoes);
    try {
      List<Future<?>> futuros = new ArrayList<>();
      for (int i = 0; i < requisicoes; i++) {
        futuros.add(
            executor.submit(
                () -> {
                  largada.await();
                  try {
                    controle.registrarTentativa(ORIGEM);
                    aceitas.incrementAndGet();
                  } catch (LimiteTentativasExcedidoException ex) {
                    bloqueadas.incrementAndGet();
                  }
                  return null;
                }));
      }
      largada.countDown();
      for (Future<?> futuro : futuros) {
        futuro.get();
      }
    } finally {
      executor.shutdownNow();
    }

    assertThat(aceitas.get()).isEqualTo(3);
    assertThat(bloqueadas.get()).isEqualTo(requisicoes - 3);
  }

  private void tentar(String origem, int vezes) {
    for (int i = 0; i < vezes; i++) {
      controle.registrarTentativa(origem);
    }
  }

  private static final class RelogioMutavel extends Clock {

    private volatile Instant agora;

    RelogioMutavel(Instant inicio) {
      this.agora = inicio;
    }

    void avancar(Duration duracao) {
      agora = agora.plus(duracao);
    }

    @Override
    public Instant instant() {
      return agora;
    }

    @Override
    public ZoneId getZone() {
      return ZoneOffset.UTC;
    }

    @Override
    public Clock withZone(ZoneId zone) {
      return this;
    }
  }
}

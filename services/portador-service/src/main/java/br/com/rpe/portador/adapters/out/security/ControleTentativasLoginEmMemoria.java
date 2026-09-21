package br.com.rpe.portador.adapters.out.security;

import br.com.rpe.portador.application.port.out.ControleTentativasLogin;
import br.com.rpe.portador.config.LoginLimiteProperties;
import br.com.rpe.portador.domain.exception.LimiteTentativasExcedidoException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

// Janela deslizante de tentativas por origem, em memória: suficiente para uma instância única do
// Portador (escopo do desafio). Com várias réplicas cada uma contaria separado - nesse caso o
// contador iria para o Redis. O mapa é LRU com teto, então origens antigas saem sozinhas. Os
// métodos são synchronized porque checar o limite e reservar a tentativa precisam ser atômicos.
@Component
public class ControleTentativasLoginEmMemoria implements ControleTentativasLogin {

  private static final Logger log = LoggerFactory.getLogger(ControleTentativasLoginEmMemoria.class);
  private static final String MENSAGEM_BLOQUEIO =
      "Muitas tentativas de login malsucedidas; tente novamente mais tarde";

  private final int maxTentativas;
  private final Duration janela;
  private final Clock clock;
  private final Map<String, Deque<Instant>> tentativasPorOrigem;

  public ControleTentativasLoginEmMemoria(LoginLimiteProperties propriedades, Clock clock) {
    this.maxTentativas = propriedades.maxFalhas();
    this.janela = propriedades.janela();
    this.clock = clock;
    int teto = propriedades.maxOrigensRastreadas();
    this.tentativasPorOrigem =
        new LinkedHashMap<>(16, 0.75f, true) {
          @Override
          protected boolean removeEldestEntry(Map.Entry<String, Deque<Instant>> eldest) {
            return size() > teto;
          }
        };
  }

  @Override
  public synchronized void registrarTentativa(String origem) {
    Instant agora = clock.instant();
    Deque<Instant> tentativas =
        tentativasPorOrigem.computeIfAbsent(origem, chave -> new ArrayDeque<>());
    descartarForaDaJanela(tentativas, agora);
    if (tentativas.size() >= maxTentativas) {
      Duration restante = Duration.between(agora, tentativas.peekFirst().plus(janela));
      log.warn("Login bloqueado por excesso de tentativas: origem={} janela={}", origem, janela);
      throw new LimiteTentativasExcedidoException(MENSAGEM_BLOQUEIO, restante);
    }
    tentativas.addLast(agora);
  }

  @Override
  public synchronized void registrarSucesso(String origem) {
    tentativasPorOrigem.remove(origem);
  }

  private void descartarForaDaJanela(Deque<Instant> tentativas, Instant agora) {
    Instant limite = agora.minus(janela);
    while (!tentativas.isEmpty() && !tentativas.peekFirst().isAfter(limite)) {
      tentativas.removeFirst();
    }
  }
}

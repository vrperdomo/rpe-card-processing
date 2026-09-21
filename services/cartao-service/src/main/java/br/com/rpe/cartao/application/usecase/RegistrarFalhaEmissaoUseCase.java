package br.com.rpe.cartao.application.usecase;

import br.com.rpe.cartao.application.port.out.EmissaoFalhaRepositorio;
import br.com.rpe.cartao.application.seguranca.Solicitante;
import br.com.rpe.cartao.domain.EmissaoFalha;
import io.micrometer.core.instrument.MeterRegistry;
import java.time.Clock;
import java.time.Instant;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Registra que a emissão de um portador falhou de vez (issue #117), para o Portador poder mostrar
 * FALHOU em vez de PENDENTE para sempre. Roda na própria transação: quando o listener o chama, a
 * transação da emissão já foi revertida, então o registro não se perde junto com ela.
 */
@Service
public class RegistrarFalhaEmissaoUseCase {

  static final String TIPO_DEFINITIVA = "definitiva";
  static final String TIPO_TENTATIVAS_ESGOTADAS = "tentativas_esgotadas";

  private final EmissaoFalhaRepositorio emissaoFalhaRepositorio;
  private final MeterRegistry meterRegistry;
  private final Clock clock;

  public RegistrarFalhaEmissaoUseCase(
      EmissaoFalhaRepositorio emissaoFalhaRepositorio, MeterRegistry meterRegistry, Clock clock) {
    this.emissaoFalhaRepositorio = emissaoFalhaRepositorio;
    this.meterRegistry = meterRegistry;
    this.clock = clock;
  }

  /** Falha que nenhuma nova tentativa resolve (produto inexistente/cancelado, schema inválido). */
  @Transactional
  public void registrarDefinitiva(
      UUID portadorId, UUID produtoId, String criadoPor, String motivo) {
    registrar(portadorId, produtoId, criadoPor, motivo, TIPO_DEFINITIVA);
  }

  /** Falha transitória na última entrega: o SQS moverá a mensagem para a DLQ em seguida. */
  @Transactional
  public void registrarTentativasEsgotadas(UUID portadorId, UUID produtoId, String criadoPor) {
    registrar(
        portadorId,
        produtoId,
        criadoPor,
        "Tentativas de emissão esgotadas: dependência indisponível",
        TIPO_TENTATIVAS_ESGOTADAS);
  }

  private void registrar(
      UUID portadorId, UUID produtoId, String criadoPor, String motivo, String tipo) {
    Instant agora = Instant.now(clock);
    emissaoFalhaRepositorio.registrar(
        new EmissaoFalha(
            portadorId, produtoId, motivo, Solicitante.donoOuLegado(criadoPor), agora));
    meterRegistry.counter("cartao.emissao.falhas", "tipo", tipo).increment();
  }
}

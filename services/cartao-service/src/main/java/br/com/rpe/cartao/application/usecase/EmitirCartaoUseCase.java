package br.com.rpe.cartao.application.usecase;

import br.com.rpe.cartao.application.port.out.CartaoRepositorio;
import br.com.rpe.cartao.application.port.out.MensagemProcessadaRepositorio;
import br.com.rpe.cartao.application.port.out.ProdutoClient;
import br.com.rpe.cartao.application.port.out.ProdutoDto;
import br.com.rpe.cartao.application.port.out.StatusProdutoExterno;
import br.com.rpe.cartao.domain.Cartao;
import br.com.rpe.cartao.domain.Pan;
import br.com.rpe.cartao.domain.Validade;
import br.com.rpe.cartao.domain.exception.RegraNegocioException;
import io.micrometer.core.instrument.MeterRegistry;
import java.time.Clock;
import java.time.Instant;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Emite o cartao a partir do evento CartaoEmissaoSolicitada (PRD 9.2, CA-01/CA-02/CA-05). Erros de
 * dependencia indisponivel (Produto Service fora, circuito aberto) propagam como
 * DependenciaIndisponivelException, sinalizando ao listener que o erro e transitorio (redelivery
 * via SQS); produto inexistente/CANCELADO propaga como RegraNegocioException, erro definitivo que o
 * listener envia direto para a DLQ.
 */
@Service
public class EmitirCartaoUseCase {

  private static final Logger log = LoggerFactory.getLogger(EmitirCartaoUseCase.class);

  private final CartaoRepositorio cartaoRepositorio;
  private final MensagemProcessadaRepositorio mensagemProcessadaRepositorio;
  private final ProdutoClient produtoClient;
  private final MeterRegistry meterRegistry;
  private final Clock clock;

  public EmitirCartaoUseCase(
      CartaoRepositorio cartaoRepositorio,
      MensagemProcessadaRepositorio mensagemProcessadaRepositorio,
      ProdutoClient produtoClient,
      MeterRegistry meterRegistry,
      Clock clock) {
    this.cartaoRepositorio = cartaoRepositorio;
    this.mensagemProcessadaRepositorio = mensagemProcessadaRepositorio;
    this.produtoClient = produtoClient;
    this.meterRegistry = meterRegistry;
    this.clock = clock;
  }

  @Transactional
  public void executar(UUID eventId, UUID portadorId, UUID produtoId, String nomeImpresso) {
    if (mensagemProcessadaRepositorio.jaProcessada(eventId)) {
      log.info("Mensagem {} ja processada, ignorando (idempotencia)", eventId);
      return;
    }
    if (cartaoRepositorio.existePorPortadorEProduto(portadorId, produtoId)) {
      log.warn(
          "Ja existe cartao para portador {} e produto {}, ignorando emissao duplicada",
          portadorId,
          produtoId);
      mensagemProcessadaRepositorio.marcarProcessada(eventId, Instant.now(clock));
      return;
    }

    ProdutoDto produto =
        produtoClient
            .buscarPorId(produtoId)
            .filter(p -> p.status() == StatusProdutoExterno.ATIVO)
            .orElseThrow(
                () ->
                    new RegraNegocioException(
                        "Produto %s inexistente ou não ATIVO".formatted(produtoId)));

    Instant agora = Instant.now(clock);
    Cartao cartao =
        Cartao.emitir(
            portadorId,
            produtoId,
            Pan.gerar(produto.bin()),
            nomeImpresso,
            Validade.gerar(agora),
            agora);
    cartaoRepositorio.salvar(cartao);
    mensagemProcessadaRepositorio.marcarProcessada(eventId, agora);
    meterRegistry.counter("cartao.emitidos").increment();
  }
}

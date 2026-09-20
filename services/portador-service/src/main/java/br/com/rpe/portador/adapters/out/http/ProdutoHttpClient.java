package br.com.rpe.portador.adapters.out.http;

import br.com.rpe.portador.application.port.out.ProdutoClient;
import br.com.rpe.portador.application.port.out.ProdutoDto;
import br.com.rpe.portador.domain.exception.DependenciaIndisponivelException;
import java.time.Duration;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletionException;
import org.springframework.stereotype.Component;

@Component
public class ProdutoHttpClient implements ProdutoClient {

  private static final Duration RETRY_AFTER_INDISPONIVEL = Duration.ofSeconds(10);

  private final ProdutoResilienteGateway gateway;

  public ProdutoHttpClient(ProdutoResilienteGateway gateway) {
    this.gateway = gateway;
  }

  @Override
  public Optional<ProdutoDto> buscarPorId(UUID produtoId) {
    try {
      return gateway.buscarPorId(produtoId).join();
    } catch (CompletionException | CancellationException ex) {
      throw new DependenciaIndisponivelException(
          "Produto Service indisponível ao consultar produto %s".formatted(produtoId),
          RETRY_AFTER_INDISPONIVEL);
    }
  }
}

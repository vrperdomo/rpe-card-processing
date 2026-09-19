package br.com.rpe.cartao.adapters.out.http;

import br.com.rpe.cartao.application.port.out.ProdutoDto;
import br.com.rpe.cartao.domain.exception.DependenciaIndisponivelException;
import java.time.Duration;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletionException;
import org.springframework.stereotype.Component;

/**
 * Gateway HTTP puro para o Produto Service. Não implementa {@code ProdutoClient} diretamente: quem
 * expõe a porta para a aplicação é o {@link
 * br.com.rpe.cartao.adapters.out.cache.ProdutoCacheAsideClient}, que decora este gateway com
 * cache-aside. Evita dois beans concorrentes para a mesma porta.
 */
@Component
public class ProdutoHttpClient {

  private static final Duration RETRY_AFTER_INDISPONIVEL = Duration.ofSeconds(10);

  private final ProdutoResilienteGateway gateway;

  public ProdutoHttpClient(ProdutoResilienteGateway gateway) {
    this.gateway = gateway;
  }

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

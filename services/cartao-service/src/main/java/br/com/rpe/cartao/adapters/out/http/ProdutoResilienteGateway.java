package br.com.rpe.cartao.adapters.out.http;

import br.com.rpe.cartao.adapters.out.http.dto.ProdutoHttpResponse;
import br.com.rpe.cartao.application.port.out.ProdutoDto;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import io.github.resilience4j.timelimiter.annotation.TimeLimiter;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;

// Bean separado de ProdutoHttpClient de propósito: chamar este método a partir de outro método
// do mesmo bean (self-invocation) faz o proxy do Spring AOP ignorar @Retry/@CircuitBreaker/
// @TimeLimiter.
@Component
public class ProdutoResilienteGateway {

  private final RestClient produtoRestClient;
  private final Executor produtoClientExecutor;

  public ProdutoResilienteGateway(RestClient produtoRestClient, Executor produtoClientExecutor) {
    this.produtoRestClient = produtoRestClient;
    this.produtoClientExecutor = produtoClientExecutor;
  }

  @Retry(name = "produto")
  @CircuitBreaker(name = "produto")
  @TimeLimiter(name = "produto")
  public CompletableFuture<Optional<ProdutoDto>> buscarPorId(UUID produtoId) {
    return CompletableFuture.supplyAsync(() -> chamarProduto(produtoId), produtoClientExecutor);
  }

  private Optional<ProdutoDto> chamarProduto(UUID produtoId) {
    try {
      ProdutoHttpResponse resposta =
          produtoRestClient
              .get()
              .uri("/api/v1/produtos/{id}", produtoId)
              .retrieve()
              .body(ProdutoHttpResponse.class);
      return Optional.ofNullable(resposta).map(this::paraDto);
    } catch (HttpClientErrorException.NotFound ex) {
      return Optional.empty();
    }
  }

  private ProdutoDto paraDto(ProdutoHttpResponse resposta) {
    return new ProdutoDto(
        resposta.id(), resposta.nome(), resposta.categoria(), resposta.bin(), resposta.status());
  }
}

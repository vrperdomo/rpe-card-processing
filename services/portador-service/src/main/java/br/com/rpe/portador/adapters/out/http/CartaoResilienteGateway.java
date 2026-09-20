package br.com.rpe.portador.adapters.out.http;

import br.com.rpe.portador.adapters.out.http.dto.CartaoHttpPaginaResponse;
import br.com.rpe.portador.adapters.out.http.dto.CartaoHttpResponse;
import br.com.rpe.portador.application.port.out.CartaoDto;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import io.github.resilience4j.timelimiter.annotation.TimeLimiter;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

// Bean separado de CartaoHttpClient de propósito: self-invocation faz o proxy do Spring AOP
// ignorar @Retry/@CircuitBreaker/@TimeLimiter (mesmo motivo do ProdutoResilienteGateway).
@Component
public class CartaoResilienteGateway {

  private final RestClient cartaoRestClient;
  private final Executor cartaoClientExecutor;

  public CartaoResilienteGateway(RestClient cartaoRestClient, Executor cartaoClientExecutor) {
    this.cartaoRestClient = cartaoRestClient;
    this.cartaoClientExecutor = cartaoClientExecutor;
  }

  @Retry(name = "cartao")
  @CircuitBreaker(name = "cartao")
  @TimeLimiter(name = "cartao")
  public CompletableFuture<Optional<CartaoDto>> buscarPorPortadorId(UUID portadorId) {
    return CompletableFuture.supplyAsync(() -> chamarCartao(portadorId), cartaoClientExecutor);
  }

  private Optional<CartaoDto> chamarCartao(UUID portadorId) {
    CartaoHttpPaginaResponse resposta =
        cartaoRestClient
            .get()
            .uri("/api/v1/cartoes?portadorId={id}&size=1", portadorId)
            .retrieve()
            .body(CartaoHttpPaginaResponse.class);
    return Optional.ofNullable(resposta)
        .map(CartaoHttpPaginaResponse::conteudo)
        .filter(conteudo -> !conteudo.isEmpty())
        .map(conteudo -> conteudo.get(0))
        .map(this::paraDto);
  }

  private CartaoDto paraDto(CartaoHttpResponse r) {
    return new CartaoDto(r.id(), r.panMascarado(), r.validade(), r.status());
  }
}

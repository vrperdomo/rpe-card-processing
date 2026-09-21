package br.com.rpe.portador.adapters.out.http;

import br.com.rpe.portador.application.port.out.CartaoClient;
import br.com.rpe.portador.application.port.out.CartaoDto;
import br.com.rpe.portador.application.port.out.FalhaEmissaoDto;
import br.com.rpe.portador.domain.exception.DependenciaIndisponivelException;
import java.time.Duration;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import org.springframework.stereotype.Component;

@Component
public class CartaoHttpClient implements CartaoClient {

  private static final Duration RETRY_AFTER_INDISPONIVEL = Duration.ofSeconds(10);

  private final CartaoResilienteGateway gateway;

  public CartaoHttpClient(CartaoResilienteGateway gateway) {
    this.gateway = gateway;
  }

  @Override
  public Optional<CartaoDto> buscarPorPortadorId(UUID portadorId) {
    return aguardar(
        gateway.buscarPorPortadorId(portadorId),
        "Cartão Service indisponível ao consultar cartão do portador %s".formatted(portadorId));
  }

  @Override
  public Optional<FalhaEmissaoDto> buscarFalhaEmissao(UUID portadorId) {
    return aguardar(
        gateway.buscarFalhaEmissao(portadorId),
        "Cartão Service indisponível ao consultar falha de emissão do portador %s"
            .formatted(portadorId));
  }

  private static <T> T aguardar(CompletableFuture<T> chamada, String mensagemIndisponivel) {
    try {
      return chamada.join();
    } catch (CompletionException | CancellationException ex) {
      throw new DependenciaIndisponivelException(mensagemIndisponivel, RETRY_AFTER_INDISPONIVEL);
    }
  }
}

package br.com.rpe.portador.domain.exception;

import java.time.Duration;

public class LimiteTentativasExcedidoException extends DomainException {

  private final Duration retryAfter;

  public LimiteTentativasExcedidoException(String message, Duration retryAfter) {
    super(message);
    this.retryAfter = retryAfter;
  }

  public Duration getRetryAfter() {
    return retryAfter;
  }
}

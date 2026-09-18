package br.com.rpe.cartao.domain.exception;

import java.time.Duration;

public class DependenciaIndisponivelException extends DomainException {

  private final Duration retryAfter;

  public DependenciaIndisponivelException(String message, Duration retryAfter) {
    super(message);
    this.retryAfter = retryAfter;
  }

  public Duration getRetryAfter() {
    return retryAfter;
  }
}

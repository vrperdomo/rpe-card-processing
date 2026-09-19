package br.com.rpe.portador.adapters.in.web;

import br.com.rpe.portador.config.CorrelationIdFilter;
import java.time.Clock;
import java.time.Instant;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.stereotype.Component;

@Component
public class ProblemDetailFactory {

  private final Clock clock;

  public ProblemDetailFactory(Clock clock) {
    this.clock = clock;
  }

  public ProblemDetail criar(HttpStatus status, String titulo, String detalhe) {
    ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detalhe);
    problem.setTitle(titulo);
    problem.setProperty("correlationId", MDC.get(CorrelationIdFilter.MDC_KEY));
    problem.setProperty("timestamp", Instant.now(clock));
    return problem;
  }
}

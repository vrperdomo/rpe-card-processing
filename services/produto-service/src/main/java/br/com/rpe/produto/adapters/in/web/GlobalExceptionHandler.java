package br.com.rpe.produto.adapters.in.web;

import br.com.rpe.produto.adapters.in.web.dto.ErroCampoResponse;
import br.com.rpe.produto.config.CorrelationIdFilter;
import br.com.rpe.produto.domain.exception.ConflitoException;
import br.com.rpe.produto.domain.exception.DependenciaIndisponivelException;
import br.com.rpe.produto.domain.exception.RecursoNaoEncontradoException;
import br.com.rpe.produto.domain.exception.RegraNegocioException;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import org.slf4j.MDC;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

  private final Clock clock;

  public GlobalExceptionHandler(Clock clock) {
    this.clock = clock;
  }

  @ExceptionHandler(RecursoNaoEncontradoException.class)
  public ResponseEntity<ProblemDetail> tratarRecursoNaoEncontrado(
      RecursoNaoEncontradoException ex) {
    return ResponseEntity.status(HttpStatus.NOT_FOUND)
        .body(problema(HttpStatus.NOT_FOUND, "Recurso não encontrado", ex.getMessage()));
  }

  @ExceptionHandler(ConflitoException.class)
  public ResponseEntity<ProblemDetail> tratarConflito(ConflitoException ex) {
    return ResponseEntity.status(HttpStatus.CONFLICT)
        .body(problema(HttpStatus.CONFLICT, "Conflito", ex.getMessage()));
  }

  /**
   * Rede de segurança para a corrida entre a checagem de nome único e a inserção (duas requisições
   * podem passar pelo pré-check antes de qualquer commit); a constraint UNIQUE do banco é quem
   * garante a consistência nesse caso raro.
   */
  @ExceptionHandler(DataIntegrityViolationException.class)
  public ResponseEntity<ProblemDetail> tratarViolacaoDeIntegridade(
      DataIntegrityViolationException ex) {
    return ResponseEntity.status(HttpStatus.CONFLICT)
        .body(
            problema(
                HttpStatus.CONFLICT,
                "Conflito",
                "O recurso já existe ou viola uma restrição de unicidade"));
  }

  @ExceptionHandler(ObjectOptimisticLockingFailureException.class)
  public ResponseEntity<ProblemDetail> tratarConflitoDeVersao(
      ObjectOptimisticLockingFailureException ex) {
    return ResponseEntity.status(HttpStatus.CONFLICT)
        .body(
            problema(
                HttpStatus.CONFLICT,
                "Conflito",
                "O recurso foi modificado por outra requisição; tente novamente"));
  }

  @ExceptionHandler(RegraNegocioException.class)
  public ResponseEntity<ProblemDetail> tratarRegraNegocio(RegraNegocioException ex) {
    return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY)
        .body(
            problema(HttpStatus.UNPROCESSABLE_ENTITY, "Regra de negócio violada", ex.getMessage()));
  }

  @ExceptionHandler(DependenciaIndisponivelException.class)
  public ResponseEntity<ProblemDetail> tratarDependenciaIndisponivel(
      DependenciaIndisponivelException ex) {
    ProblemDetail problem =
        problema(HttpStatus.SERVICE_UNAVAILABLE, "Dependência indisponível", ex.getMessage());
    return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
        .header(HttpHeaders.RETRY_AFTER, String.valueOf(ex.getRetryAfter().toSeconds()))
        .body(problem);
  }

  @Override
  protected ResponseEntity<Object> handleMethodArgumentNotValid(
      MethodArgumentNotValidException ex,
      HttpHeaders headers,
      HttpStatusCode status,
      WebRequest request) {
    List<ErroCampoResponse> erros =
        ex.getBindingResult().getFieldErrors().stream()
            .map(erro -> new ErroCampoResponse(erro.getField(), erro.getDefaultMessage()))
            .toList();
    ProblemDetail problem =
        problema(HttpStatus.BAD_REQUEST, "Payload inválido", "Um ou mais campos são inválidos");
    problem.setProperty("errors", erros);
    return ResponseEntity.badRequest().body(problem);
  }

  private ProblemDetail problema(HttpStatus status, String titulo, String detalhe) {
    ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detalhe);
    problem.setTitle(titulo);
    problem.setProperty("correlationId", MDC.get(CorrelationIdFilter.MDC_KEY));
    problem.setProperty("timestamp", Instant.now(clock));
    return problem;
  }
}

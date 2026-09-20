package br.com.rpe.portador.adapters.in.web;

import br.com.rpe.portador.adapters.in.web.dto.ErroCampoResponse;
import br.com.rpe.portador.domain.exception.ConflitoException;
import br.com.rpe.portador.domain.exception.CredenciaisInvalidasException;
import br.com.rpe.portador.domain.exception.DependenciaIndisponivelException;
import br.com.rpe.portador.domain.exception.RecursoNaoEncontradoException;
import br.com.rpe.portador.domain.exception.RegraNegocioException;
import java.util.List;
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

  private final ProblemDetailFactory problemDetailFactory;

  public GlobalExceptionHandler(ProblemDetailFactory problemDetailFactory) {
    this.problemDetailFactory = problemDetailFactory;
  }

  @ExceptionHandler(CredenciaisInvalidasException.class)
  public ResponseEntity<ProblemDetail> tratarCredenciaisInvalidas(
      CredenciaisInvalidasException ex) {
    return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
        .body(
            problemDetailFactory.criar(
                HttpStatus.UNAUTHORIZED, "Não autenticado", ex.getMessage()));
  }

  @ExceptionHandler(RegraNegocioException.class)
  public ResponseEntity<ProblemDetail> tratarRegraNegocio(RegraNegocioException ex) {
    return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY)
        .body(
            problemDetailFactory.criar(
                HttpStatus.UNPROCESSABLE_ENTITY, "Regra de negócio violada", ex.getMessage()));
  }

  @ExceptionHandler(ConflitoException.class)
  public ResponseEntity<ProblemDetail> tratarConflito(ConflitoException ex) {
    return ResponseEntity.status(HttpStatus.CONFLICT)
        .body(problemDetailFactory.criar(HttpStatus.CONFLICT, "Conflito", ex.getMessage()));
  }

  @ExceptionHandler(RecursoNaoEncontradoException.class)
  public ResponseEntity<ProblemDetail> tratarRecursoNaoEncontrado(
      RecursoNaoEncontradoException ex) {
    return ResponseEntity.status(HttpStatus.NOT_FOUND)
        .body(
            problemDetailFactory.criar(
                HttpStatus.NOT_FOUND, "Recurso não encontrado", ex.getMessage()));
  }

  @ExceptionHandler(ObjectOptimisticLockingFailureException.class)
  public ResponseEntity<ProblemDetail> tratarConflitoDeVersao(
      ObjectOptimisticLockingFailureException ex) {
    return ResponseEntity.status(HttpStatus.CONFLICT)
        .body(
            problemDetailFactory.criar(
                HttpStatus.CONFLICT,
                "Conflito",
                "O recurso foi modificado por outra requisição; tente novamente"));
  }

  /**
   * Rede de segurança para a corrida entre a checagem de CPF único e o insert (duas requisições
   * podem passar pelo pré-check antes de qualquer commit); a constraint UNIQUE do banco é quem
   * garante a consistência nesse caso raro.
   */
  @ExceptionHandler(DataIntegrityViolationException.class)
  public ResponseEntity<ProblemDetail> tratarViolacaoDeIntegridade(
      DataIntegrityViolationException ex) {
    return ResponseEntity.status(HttpStatus.CONFLICT)
        .body(
            problemDetailFactory.criar(
                HttpStatus.CONFLICT,
                "Conflito",
                "O recurso já existe ou viola uma restrição de unicidade"));
  }

  @ExceptionHandler(DependenciaIndisponivelException.class)
  public ResponseEntity<ProblemDetail> tratarDependenciaIndisponivel(
      DependenciaIndisponivelException ex) {
    ProblemDetail problem =
        problemDetailFactory.criar(
            HttpStatus.SERVICE_UNAVAILABLE, "Dependência indisponível", ex.getMessage());
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
        problemDetailFactory.criar(
            HttpStatus.BAD_REQUEST, "Payload inválido", "Um ou mais campos são inválidos");
    problem.setProperty("errors", erros);
    return ResponseEntity.badRequest().body(problem);
  }
}

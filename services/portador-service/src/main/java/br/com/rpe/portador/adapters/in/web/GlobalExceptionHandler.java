package br.com.rpe.portador.adapters.in.web;

import br.com.rpe.portador.adapters.in.web.dto.ErroCampoResponse;
import br.com.rpe.portador.domain.exception.CredenciaisInvalidasException;
import java.util.List;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
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

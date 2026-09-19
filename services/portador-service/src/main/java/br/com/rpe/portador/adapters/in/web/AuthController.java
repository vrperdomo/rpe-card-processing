package br.com.rpe.portador.adapters.in.web;

import br.com.rpe.portador.adapters.in.web.dto.LoginRequest;
import br.com.rpe.portador.adapters.in.web.dto.LoginResponse;
import br.com.rpe.portador.application.port.out.GeradorToken;
import br.com.rpe.portador.application.usecase.AutenticarUseCase;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

  private final AutenticarUseCase autenticarUseCase;

  public AuthController(AutenticarUseCase autenticarUseCase) {
    this.autenticarUseCase = autenticarUseCase;
  }

  @PostMapping("/login")
  public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
    GeradorToken.Token token = autenticarUseCase.executar(request.username(), request.password());
    return ResponseEntity.ok(LoginResponse.deToken(token.valor(), token.expiraEmSegundos()));
  }
}

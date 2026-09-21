package br.com.rpe.portador.adapters.in.web;

import br.com.rpe.portador.adapters.in.web.dto.LoginRequest;
import br.com.rpe.portador.adapters.in.web.dto.LoginResponse;
import br.com.rpe.portador.application.port.out.GeradorToken;
import br.com.rpe.portador.application.usecase.AutenticarUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import jakarta.servlet.http.HttpServletRequest;
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

  // A origem vem de getRemoteAddr(): com server.forward-headers-strategy=native o Tomcat já
  // troca o IP do proxy pelo do cliente (X-Forwarded-For), só quando o proxy é confiável.
  // @SecurityRequirements vazio: o login é o único endpoint público, então não leva o cadeado que o
  // esquema global põe nos demais.
  @PostMapping("/login")
  @SecurityRequirements
  @Operation(
      summary = "Autentica e emite o JWT",
      description =
          "Usuário seed de desenvolvimento (README). Copie o accessToken e cole no botão"
              + " Authorize de qualquer um dos 3 Swaggers.")
  public ResponseEntity<LoginResponse> login(
      @Valid @RequestBody LoginRequest request, HttpServletRequest http) {
    GeradorToken.Token token =
        autenticarUseCase.executar(request.username(), request.password(), http.getRemoteAddr());
    return ResponseEntity.ok(LoginResponse.deToken(token.valor(), token.expiraEmSegundos()));
  }
}

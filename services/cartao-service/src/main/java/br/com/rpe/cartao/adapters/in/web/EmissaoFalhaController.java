package br.com.rpe.cartao.adapters.in.web;

import br.com.rpe.cartao.adapters.in.web.dto.EmissaoFalhaResponse;
import br.com.rpe.cartao.adapters.in.web.security.SolicitanteJwt;
import br.com.rpe.cartao.application.usecase.BuscarFalhaEmissaoUseCase;
import br.com.rpe.cartao.domain.EmissaoFalha;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.UUID;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/emissao-falhas")
@Tag(name = "Falhas de emissão", description = "Consulta da falha de emissão de um portador")
public class EmissaoFalhaController {

  private final BuscarFalhaEmissaoUseCase buscarFalhaEmissaoUseCase;

  public EmissaoFalhaController(BuscarFalhaEmissaoUseCase buscarFalhaEmissaoUseCase) {
    this.buscarFalhaEmissaoUseCase = buscarFalhaEmissaoUseCase;
  }

  @GetMapping("/{portadorId}")
  @Operation(
      summary = "Busca a falha de emissão do cartão de um portador",
      description =
          "Retorna 404 se não houver falha registrada ou se ela pertencer a outro usuário. Usado"
              + " pelo Portador na consulta agregada; o Nginx do frontend não o expõe.")
  public EmissaoFalhaResponse buscar(
      @PathVariable UUID portadorId, @AuthenticationPrincipal Jwt jwt) {
    EmissaoFalha falha = buscarFalhaEmissaoUseCase.executar(portadorId, SolicitanteJwt.de(jwt));
    return new EmissaoFalhaResponse(
        falha.portadorId(), falha.produtoId(), falha.motivo(), falha.ocorridaEm());
  }
}

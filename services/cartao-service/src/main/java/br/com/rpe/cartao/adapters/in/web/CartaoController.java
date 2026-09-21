package br.com.rpe.cartao.adapters.in.web;

import br.com.rpe.cartao.adapters.in.web.dto.AlterarStatusCartaoRequest;
import br.com.rpe.cartao.adapters.in.web.dto.CartaoResponse;
import br.com.rpe.cartao.adapters.in.web.dto.PaginaResponse;
import br.com.rpe.cartao.adapters.in.web.mapper.CartaoWebMapper;
import br.com.rpe.cartao.adapters.in.web.security.SolicitanteJwt;
import br.com.rpe.cartao.application.usecase.AlterarStatusCartaoUseCase;
import br.com.rpe.cartao.application.usecase.BuscarCartaoUseCase;
import br.com.rpe.cartao.application.usecase.CartaoComProduto;
import br.com.rpe.cartao.application.usecase.ListarCartoesPorPortadorUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/cartoes")
@Tag(name = "Cartões", description = "Consulta e alteração de status de cartões")
public class CartaoController {

  private final BuscarCartaoUseCase buscarCartaoUseCase;
  private final ListarCartoesPorPortadorUseCase listarCartoesPorPortadorUseCase;
  private final AlterarStatusCartaoUseCase alterarStatusCartaoUseCase;
  private final CartaoWebMapper mapper;

  public CartaoController(
      BuscarCartaoUseCase buscarCartaoUseCase,
      ListarCartoesPorPortadorUseCase listarCartoesPorPortadorUseCase,
      AlterarStatusCartaoUseCase alterarStatusCartaoUseCase,
      CartaoWebMapper mapper) {
    this.buscarCartaoUseCase = buscarCartaoUseCase;
    this.listarCartoesPorPortadorUseCase = listarCartoesPorPortadorUseCase;
    this.alterarStatusCartaoUseCase = alterarStatusCartaoUseCase;
    this.mapper = mapper;
  }

  @GetMapping("/{id}")
  @Operation(
      summary = "Busca um cartão por id, com dados do produto",
      description =
          "Só o dono lê. Retorna 404 se o cartão não existir ou pertencer a outro usuário.")
  public CartaoResponse buscar(@PathVariable UUID id, @AuthenticationPrincipal Jwt jwt) {
    CartaoComProduto resultado = buscarCartaoUseCase.executar(id, SolicitanteJwt.de(jwt));
    return mapper.paraResponse(resultado.cartao(), resultado.produto());
  }

  @GetMapping
  @Operation(
      summary = "Lista cartões paginados de um portador, com dados do produto",
      description = "Lista apenas os cartões do usuário autenticado.")
  public PaginaResponse<CartaoResponse> listar(
      @RequestParam UUID portadorId, Pageable pageable, @AuthenticationPrincipal Jwt jwt) {
    Page<CartaoResponse> pagina =
        listarCartoesPorPortadorUseCase
            .executar(portadorId, pageable, SolicitanteJwt.de(jwt))
            .map(resultado -> mapper.paraResponse(resultado.cartao(), resultado.produto()));
    return PaginaResponse.de(pagina);
  }

  @PatchMapping("/{id}/status")
  @Operation(
      summary = "Altera o status de um cartão (ATIVO, BLOQUEADO, CANCELADO)",
      description =
          "Só o dono altera. Retorna 404 se o cartão não existir ou pertencer a outro usuário.")
  public CartaoResponse alterarStatus(
      @PathVariable UUID id,
      @Valid @RequestBody AlterarStatusCartaoRequest request,
      @AuthenticationPrincipal Jwt jwt) {
    var cartao = alterarStatusCartaoUseCase.executar(id, request.status(), SolicitanteJwt.de(jwt));
    return mapper.paraResponse(cartao);
  }
}

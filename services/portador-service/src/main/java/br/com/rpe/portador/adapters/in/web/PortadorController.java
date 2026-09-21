package br.com.rpe.portador.adapters.in.web;

import br.com.rpe.portador.adapters.in.web.dto.AlterarStatusPortadorRequest;
import br.com.rpe.portador.adapters.in.web.dto.CadastrarPortadorRequest;
import br.com.rpe.portador.adapters.in.web.dto.PortadorCompletoResponse;
import br.com.rpe.portador.adapters.in.web.dto.PortadorResponse;
import br.com.rpe.portador.adapters.in.web.mapper.PortadorCompletoWebMapper;
import br.com.rpe.portador.adapters.in.web.mapper.PortadorWebMapper;
import br.com.rpe.portador.adapters.in.web.security.SolicitanteJwt;
import br.com.rpe.portador.application.usecase.AlterarStatusPortadorUseCase;
import br.com.rpe.portador.application.usecase.BuscarPortadorCompletoUseCase;
import br.com.rpe.portador.application.usecase.BuscarPortadorUseCase;
import br.com.rpe.portador.application.usecase.CadastrarPortadorUseCase;
import br.com.rpe.portador.config.CorrelationIdFilter;
import br.com.rpe.portador.domain.Cpf;
import br.com.rpe.portador.domain.Portador;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.UUID;
import org.slf4j.MDC;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/portadores")
@Tag(name = "Portadores", description = "Cadastro de portadores")
public class PortadorController {

  private final CadastrarPortadorUseCase cadastrarPortadorUseCase;
  private final BuscarPortadorUseCase buscarPortadorUseCase;
  private final BuscarPortadorCompletoUseCase buscarPortadorCompletoUseCase;
  private final AlterarStatusPortadorUseCase alterarStatusPortadorUseCase;
  private final PortadorWebMapper mapper;
  private final PortadorCompletoWebMapper completoMapper;

  public PortadorController(
      CadastrarPortadorUseCase cadastrarPortadorUseCase,
      BuscarPortadorUseCase buscarPortadorUseCase,
      BuscarPortadorCompletoUseCase buscarPortadorCompletoUseCase,
      AlterarStatusPortadorUseCase alterarStatusPortadorUseCase,
      PortadorWebMapper mapper,
      PortadorCompletoWebMapper completoMapper) {
    this.cadastrarPortadorUseCase = cadastrarPortadorUseCase;
    this.buscarPortadorUseCase = buscarPortadorUseCase;
    this.buscarPortadorCompletoUseCase = buscarPortadorCompletoUseCase;
    this.alterarStatusPortadorUseCase = alterarStatusPortadorUseCase;
    this.mapper = mapper;
    this.completoMapper = completoMapper;
  }

  @PostMapping
  @Operation(
      summary = "Cadastra um novo portador",
      description = "O usuário autenticado (sub do JWT) passa a ser o dono do portador.")
  public ResponseEntity<PortadorResponse> cadastrar(
      @Valid @RequestBody CadastrarPortadorRequest request, @AuthenticationPrincipal Jwt jwt) {
    Portador portador =
        cadastrarPortadorUseCase.executar(
            request.nome(),
            Cpf.of(request.cpf()),
            request.dataNascimento(),
            request.produtoId(),
            SolicitanteJwt.de(jwt),
            MDC.get(CorrelationIdFilter.MDC_KEY));
    URI location = URI.create("/api/v1/portadores/%s".formatted(portador.getId()));
    return ResponseEntity.created(location).body(mapper.paraResponse(portador));
  }

  @GetMapping("/{id}")
  @Operation(
      summary = "Busca um portador por id",
      description =
          "Só o dono lê. Retorna 404 se o portador não existir ou pertencer a outro usuário.")
  public PortadorResponse buscar(@PathVariable UUID id, @AuthenticationPrincipal Jwt jwt) {
    return mapper.paraResponse(buscarPortadorUseCase.executar(id, SolicitanteJwt.de(jwt)));
  }

  @GetMapping("/{id}/completo")
  @Operation(
      summary = "Busca portador + cartão + produto agregados (resposta degradável)",
      description =
          "Só o dono lê. Retorna 404 se o portador não existir ou pertencer a outro usuário. O campo"
              + " emissao é CONCLUIDA, PENDENTE, FALHOU (com falhaEmissao: motivo e horário) ou"
              + " DESCONHECIDA (Cartão indisponível).")
  public PortadorCompletoResponse buscarCompleto(
      @PathVariable UUID id, @AuthenticationPrincipal Jwt jwt) {
    return completoMapper.paraResponse(
        buscarPortadorCompletoUseCase.executar(id, SolicitanteJwt.de(jwt)));
  }

  @PatchMapping("/{id}/status")
  @Operation(
      summary = "Altera o status de um portador (ATIVO, BLOQUEADO, CANCELADO)",
      description =
          "Só o dono altera. Retorna 404 se o portador não existir ou pertencer a outro usuário.")
  public PortadorResponse alterarStatus(
      @PathVariable UUID id,
      @Valid @RequestBody AlterarStatusPortadorRequest request,
      @AuthenticationPrincipal Jwt jwt) {
    return mapper.paraResponse(
        alterarStatusPortadorUseCase.executar(id, request.status(), SolicitanteJwt.de(jwt)));
  }
}

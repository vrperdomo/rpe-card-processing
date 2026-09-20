package br.com.rpe.portador.adapters.in.web;

import br.com.rpe.portador.adapters.in.web.dto.AlterarStatusPortadorRequest;
import br.com.rpe.portador.adapters.in.web.dto.CadastrarPortadorRequest;
import br.com.rpe.portador.adapters.in.web.dto.PortadorResponse;
import br.com.rpe.portador.adapters.in.web.mapper.PortadorWebMapper;
import br.com.rpe.portador.application.usecase.AlterarStatusPortadorUseCase;
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
  private final AlterarStatusPortadorUseCase alterarStatusPortadorUseCase;
  private final PortadorWebMapper mapper;

  public PortadorController(
      CadastrarPortadorUseCase cadastrarPortadorUseCase,
      BuscarPortadorUseCase buscarPortadorUseCase,
      AlterarStatusPortadorUseCase alterarStatusPortadorUseCase,
      PortadorWebMapper mapper) {
    this.cadastrarPortadorUseCase = cadastrarPortadorUseCase;
    this.buscarPortadorUseCase = buscarPortadorUseCase;
    this.alterarStatusPortadorUseCase = alterarStatusPortadorUseCase;
    this.mapper = mapper;
  }

  @PostMapping
  @Operation(summary = "Cadastra um novo portador")
  public ResponseEntity<PortadorResponse> cadastrar(
      @Valid @RequestBody CadastrarPortadorRequest request) {
    Portador portador =
        cadastrarPortadorUseCase.executar(
            request.nome(),
            Cpf.of(request.cpf()),
            request.dataNascimento(),
            request.produtoId(),
            MDC.get(CorrelationIdFilter.MDC_KEY));
    URI location = URI.create("/api/v1/portadores/%s".formatted(portador.getId()));
    return ResponseEntity.created(location).body(mapper.paraResponse(portador));
  }

  @GetMapping("/{id}")
  @Operation(summary = "Busca um portador por id")
  public PortadorResponse buscar(@PathVariable UUID id) {
    return mapper.paraResponse(buscarPortadorUseCase.executar(id));
  }

  @PatchMapping("/{id}/status")
  @Operation(summary = "Altera o status de um portador (ATIVO, BLOQUEADO, CANCELADO)")
  public PortadorResponse alterarStatus(
      @PathVariable UUID id, @Valid @RequestBody AlterarStatusPortadorRequest request) {
    return mapper.paraResponse(alterarStatusPortadorUseCase.executar(id, request.status()));
  }
}

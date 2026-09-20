package br.com.rpe.portador.adapters.in.web;

import br.com.rpe.portador.adapters.in.web.dto.CadastrarPortadorRequest;
import br.com.rpe.portador.adapters.in.web.dto.PortadorResponse;
import br.com.rpe.portador.adapters.in.web.mapper.PortadorWebMapper;
import br.com.rpe.portador.application.usecase.CadastrarPortadorUseCase;
import br.com.rpe.portador.config.CorrelationIdFilter;
import br.com.rpe.portador.domain.Cpf;
import br.com.rpe.portador.domain.Portador;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.net.URI;
import org.slf4j.MDC;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/portadores")
@Tag(name = "Portadores", description = "Cadastro de portadores")
public class PortadorController {

  private final CadastrarPortadorUseCase cadastrarPortadorUseCase;
  private final PortadorWebMapper mapper;

  public PortadorController(
      CadastrarPortadorUseCase cadastrarPortadorUseCase, PortadorWebMapper mapper) {
    this.cadastrarPortadorUseCase = cadastrarPortadorUseCase;
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
}

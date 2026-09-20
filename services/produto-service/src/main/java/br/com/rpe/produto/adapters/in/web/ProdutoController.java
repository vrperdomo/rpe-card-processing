package br.com.rpe.produto.adapters.in.web;

import br.com.rpe.produto.adapters.in.web.dto.AlterarStatusProdutoRequest;
import br.com.rpe.produto.adapters.in.web.dto.CriarProdutoRequest;
import br.com.rpe.produto.adapters.in.web.dto.PaginaResponse;
import br.com.rpe.produto.adapters.in.web.dto.ProdutoResponse;
import br.com.rpe.produto.adapters.in.web.mapper.ProdutoWebMapper;
import br.com.rpe.produto.application.usecase.AlterarStatusProdutoUseCase;
import br.com.rpe.produto.application.usecase.BuscarProdutoUseCase;
import br.com.rpe.produto.application.usecase.CriarProdutoUseCase;
import br.com.rpe.produto.application.usecase.ListarProdutosUseCase;
import br.com.rpe.produto.config.CorrelationIdFilter;
import br.com.rpe.produto.domain.Produto;
import br.com.rpe.produto.domain.StatusProduto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.UUID;
import org.slf4j.MDC;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/produtos")
@Tag(name = "Produtos", description = "Catálogo de produtos de cartão")
public class ProdutoController {

  private final CriarProdutoUseCase criarProdutoUseCase;
  private final BuscarProdutoUseCase buscarProdutoUseCase;
  private final ListarProdutosUseCase listarProdutosUseCase;
  private final AlterarStatusProdutoUseCase alterarStatusProdutoUseCase;
  private final ProdutoWebMapper mapper;

  public ProdutoController(
      CriarProdutoUseCase criarProdutoUseCase,
      BuscarProdutoUseCase buscarProdutoUseCase,
      ListarProdutosUseCase listarProdutosUseCase,
      AlterarStatusProdutoUseCase alterarStatusProdutoUseCase,
      ProdutoWebMapper mapper) {
    this.criarProdutoUseCase = criarProdutoUseCase;
    this.buscarProdutoUseCase = buscarProdutoUseCase;
    this.listarProdutosUseCase = listarProdutosUseCase;
    this.alterarStatusProdutoUseCase = alterarStatusProdutoUseCase;
    this.mapper = mapper;
  }

  @PostMapping
  @Operation(summary = "Cadastra um novo produto")
  public ResponseEntity<ProdutoResponse> criar(@Valid @RequestBody CriarProdutoRequest request) {
    Produto produto =
        criarProdutoUseCase.executar(
            request.nome(), request.descricao(), request.categoria(), request.bin());
    URI location = URI.create("/api/v1/produtos/%s".formatted(produto.getId()));
    return ResponseEntity.created(location).body(mapper.paraResponse(produto));
  }

  @GetMapping("/{id}")
  @Operation(summary = "Busca um produto por id")
  public ProdutoResponse buscar(@PathVariable UUID id) {
    return mapper.paraResponse(buscarProdutoUseCase.executar(id));
  }

  @GetMapping
  @Operation(summary = "Lista produtos paginados, com filtro opcional por status")
  public PaginaResponse<ProdutoResponse> listar(
      @RequestParam(required = false) StatusProduto status, Pageable pageable) {
    Page<ProdutoResponse> pagina =
        listarProdutosUseCase.executar(status, pageable).map(mapper::paraResponse);
    return PaginaResponse.de(pagina);
  }

  @PatchMapping("/{id}/status")
  @Operation(summary = "Altera o status de um produto (ATIVO → CANCELADO)")
  public ProdutoResponse alterarStatus(
      @PathVariable UUID id, @Valid @RequestBody AlterarStatusProdutoRequest request) {
    String correlationId = MDC.get(CorrelationIdFilter.MDC_KEY);
    return mapper.paraResponse(
        alterarStatusProdutoUseCase.executar(id, request.status(), correlationId));
  }
}

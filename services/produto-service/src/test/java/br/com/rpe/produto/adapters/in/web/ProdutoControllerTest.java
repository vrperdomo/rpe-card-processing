package br.com.rpe.produto.adapters.in.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.com.rpe.produto.adapters.in.web.mapper.ProdutoWebMapperImpl;
import br.com.rpe.produto.adapters.in.web.security.JwtAccessDeniedHandler;
import br.com.rpe.produto.adapters.in.web.security.JwtAuthEntryPoint;
import br.com.rpe.produto.application.usecase.AlterarStatusProdutoUseCase;
import br.com.rpe.produto.application.usecase.BuscarProdutoUseCase;
import br.com.rpe.produto.application.usecase.CriarProdutoUseCase;
import br.com.rpe.produto.application.usecase.ListarProdutosUseCase;
import br.com.rpe.produto.config.ClockConfig;
import br.com.rpe.produto.config.SecurityConfig;
import br.com.rpe.produto.domain.CategoriaProduto;
import br.com.rpe.produto.domain.Produto;
import br.com.rpe.produto.domain.StatusProduto;
import br.com.rpe.produto.domain.exception.ConflitoException;
import br.com.rpe.produto.domain.exception.RecursoNaoEncontradoException;
import br.com.rpe.produto.domain.exception.RegraNegocioException;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(ProdutoController.class)
@Import({
  ProdutoWebMapperImpl.class,
  ClockConfig.class,
  SecurityConfig.class,
  ProblemDetailFactory.class,
  JwtAuthEntryPoint.class,
  JwtAccessDeniedHandler.class
})
class ProdutoControllerTest {

  private static final Instant AGORA = Instant.parse("2026-09-17T12:00:00Z");

  @Autowired private MockMvc mockMvc;

  @MockitoBean private CriarProdutoUseCase criarProdutoUseCase;
  @MockitoBean private BuscarProdutoUseCase buscarProdutoUseCase;
  @MockitoBean private ListarProdutosUseCase listarProdutosUseCase;
  @MockitoBean private AlterarStatusProdutoUseCase alterarStatusProdutoUseCase;

  @Test
  void deveCriarProdutoERetornar201ComLocation() throws Exception {
    Produto produto = Produto.criar("Gold", "descricao", CategoriaProduto.GOLD, "123456", AGORA);
    when(criarProdutoUseCase.executar("Gold", "descricao", CategoriaProduto.GOLD, "123456"))
        .thenReturn(produto);

    mockMvc
        .perform(
            post("/api/v1/produtos")
                .with(jwt())
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"nome":"Gold","descricao":"descricao","categoria":"GOLD","bin":"123456"}
                    """))
        .andExpect(status().isCreated())
        .andExpect(header().string("Location", "/api/v1/produtos/" + produto.getId()))
        .andExpect(jsonPath("$.nome").value("Gold"));
  }

  @Test
  void deveRetornar401QuandoSemToken() throws Exception {
    mockMvc
        .perform(get("/api/v1/produtos/{id}", UUID.randomUUID()))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.title").value("Não autenticado"));
  }

  @Test
  void deveRetornar400QuandoNomeEmBranco() throws Exception {
    mockMvc
        .perform(
            post("/api/v1/produtos")
                .with(jwt())
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"nome":"","categoria":"GOLD","bin":"123456"}
                    """))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.errors[0].field").value("nome"));
  }

  @Test
  void deveRetornar409QuandoNomeDuplicado() throws Exception {
    when(criarProdutoUseCase.executar(any(), any(), any(), any()))
        .thenThrow(new ConflitoException("Já existe um produto com esse nome"));

    mockMvc
        .perform(
            post("/api/v1/produtos")
                .with(jwt())
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"nome":"Gold","categoria":"GOLD","bin":"123456"}
                    """))
        .andExpect(status().isConflict());
  }

  @Test
  void deveRetornar409QuandoConstraintDeUnicidadeViolarNaCorrida() throws Exception {
    when(criarProdutoUseCase.executar(any(), any(), any(), any()))
        .thenThrow(new DataIntegrityViolationException("uk_produto_nome"));

    mockMvc
        .perform(
            post("/api/v1/produtos")
                .with(jwt())
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"nome":"Gold","categoria":"GOLD","bin":"123456"}
                    """))
        .andExpect(status().isConflict());
  }

  @Test
  void deveRetornar409QuandoEscritaConcorrenteCausaOptimisticLock() throws Exception {
    UUID id = UUID.randomUUID();
    when(alterarStatusProdutoUseCase.executar(id, StatusProduto.CANCELADO))
        .thenThrow(new ObjectOptimisticLockingFailureException(Produto.class, id));

    mockMvc
        .perform(
            patch("/api/v1/produtos/{id}/status", id)
                .with(jwt())
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"status":"CANCELADO"}
                    """))
        .andExpect(status().isConflict());
  }

  @Test
  void deveBuscarProdutoPorId() throws Exception {
    Produto produto = Produto.criar("Gold", "descricao", CategoriaProduto.GOLD, "123456", AGORA);
    when(buscarProdutoUseCase.executar(produto.getId())).thenReturn(produto);

    mockMvc
        .perform(get("/api/v1/produtos/{id}", produto.getId()).with(jwt()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(produto.getId().toString()));
  }

  @Test
  void deveRetornar404QuandoProdutoNaoEncontrado() throws Exception {
    UUID id = UUID.randomUUID();
    when(buscarProdutoUseCase.executar(id))
        .thenThrow(new RecursoNaoEncontradoException("Produto não encontrado"));

    mockMvc
        .perform(get("/api/v1/produtos/{id}", id).with(jwt()))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.title").value("Recurso não encontrado"));
  }

  @Test
  void deveListarProdutosPaginados() throws Exception {
    Produto produto = Produto.criar("Gold", "descricao", CategoriaProduto.GOLD, "123456", AGORA);
    Page<Produto> pagina = new PageImpl<>(List.of(produto), PageRequest.of(0, 20), 1);
    when(listarProdutosUseCase.executar(eq(StatusProduto.ATIVO), any())).thenReturn(pagina);

    mockMvc
        .perform(get("/api/v1/produtos").with(jwt()).param("status", "ATIVO"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.conteudo[0].nome").value("Gold"))
        .andExpect(jsonPath("$.totalElementos").value(1));
  }

  @Test
  void deveAlterarStatusParaCancelado() throws Exception {
    Produto produto = Produto.criar("Gold", "descricao", CategoriaProduto.GOLD, "123456", AGORA);
    produto.cancelar(AGORA.plusSeconds(60));
    when(alterarStatusProdutoUseCase.executar(produto.getId(), StatusProduto.CANCELADO))
        .thenReturn(produto);

    mockMvc
        .perform(
            patch("/api/v1/produtos/{id}/status", produto.getId())
                .with(jwt())
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"status":"CANCELADO"}
                    """))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("CANCELADO"));
  }

  @Test
  void deveRetornar422QuandoTransicaoDeStatusInvalida() throws Exception {
    UUID id = UUID.randomUUID();
    when(alterarStatusProdutoUseCase.executar(id, StatusProduto.ATIVO))
        .thenThrow(new RegraNegocioException("Transição não permitida"));

    mockMvc
        .perform(
            patch("/api/v1/produtos/{id}/status", id)
                .with(jwt())
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"status":"ATIVO"}
                    """))
        .andExpect(status().isUnprocessableEntity());
  }
}

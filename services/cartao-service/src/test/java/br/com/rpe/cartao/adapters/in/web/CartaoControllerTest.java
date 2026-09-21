package br.com.rpe.cartao.adapters.in.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.com.rpe.cartao.adapters.in.web.mapper.CartaoWebMapper;
import br.com.rpe.cartao.application.port.out.ProdutoDto;
import br.com.rpe.cartao.application.port.out.StatusProdutoExterno;
import br.com.rpe.cartao.application.seguranca.Solicitante;
import br.com.rpe.cartao.application.usecase.AlterarStatusCartaoUseCase;
import br.com.rpe.cartao.application.usecase.BuscarCartaoUseCase;
import br.com.rpe.cartao.application.usecase.CartaoComProduto;
import br.com.rpe.cartao.application.usecase.ListarCartoesPorPortadorUseCase;
import br.com.rpe.cartao.config.ClockConfig;
import br.com.rpe.cartao.config.SecurityConfig;
import br.com.rpe.cartao.domain.Cartao;
import br.com.rpe.cartao.domain.Pan;
import br.com.rpe.cartao.domain.StatusCartao;
import br.com.rpe.cartao.domain.Validade;
import br.com.rpe.cartao.domain.exception.DependenciaIndisponivelException;
import br.com.rpe.cartao.domain.exception.RecursoNaoEncontradoException;
import br.com.rpe.cartao.domain.exception.RegraNegocioException;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.JwtRequestPostProcessor;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(CartaoController.class)
@Import({
  CartaoWebMapper.class,
  ClockConfig.class,
  SecurityConfig.class,
  ProblemDetailFactory.class,
  GlobalExceptionHandler.class,
  br.com.rpe.cartao.adapters.in.web.security.JwtAuthEntryPoint.class,
  br.com.rpe.cartao.adapters.in.web.security.JwtAccessDeniedHandler.class
})
@TestPropertySource(
    properties = {
      "rpe.jwt.secret=segredo-de-teste-com-pelo-menos-32-caracteres",
      "rpe.jwt.issuer=https://portador-service.rpe.local",
      "rpe.jwt.audience=rpe-api"
    })
class CartaoControllerTest {

  private static final Instant AGORA = Instant.parse("2026-09-20T12:00:00Z");

  private static final Solicitante ADMIN = Solicitante.deUsuario("admin");

  // JWT de usuário: o sub vira o Solicitante que o controller entrega ao caso de uso (ADR-009).
  private static JwtRequestPostProcessor admin() {
    return jwt().jwt(token -> token.subject("admin"));
  }

  @Autowired private MockMvc mockMvc;

  @MockitoBean private BuscarCartaoUseCase buscarCartaoUseCase;
  @MockitoBean private ListarCartoesPorPortadorUseCase listarCartoesPorPortadorUseCase;
  @MockitoBean private AlterarStatusCartaoUseCase alterarStatusCartaoUseCase;

  private Cartao cartao(UUID portadorId, UUID produtoId) {
    return Cartao.emitir(
        portadorId,
        produtoId,
        Pan.of("4532015112830366"),
        "VICTOR RODRIGUES",
        Validade.gerar(AGORA),
        "admin",
        AGORA);
  }

  @Test
  void deveBuscarCartaoPorIdComPanMascaradoEProduto() throws Exception {
    UUID portadorId = UUID.randomUUID();
    UUID produtoId = UUID.randomUUID();
    Cartao cartao = cartao(portadorId, produtoId);
    ProdutoDto produto =
        new ProdutoDto(produtoId, "Gold", "GOLD", "453201", StatusProdutoExterno.ATIVO);
    when(buscarCartaoUseCase.executar(cartao.getId(), ADMIN))
        .thenReturn(new CartaoComProduto(cartao, Optional.of(produto)));

    mockMvc
        .perform(get("/api/v1/cartoes/{id}", cartao.getId()).with(admin()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.panMascarado").value("**** **** **** 0366"))
        .andExpect(jsonPath("$.produto.nome").value("Gold"));
  }

  @Test
  void deveRetornar404QuandoCartaoNaoEncontrado() throws Exception {
    UUID id = UUID.randomUUID();
    when(buscarCartaoUseCase.executar(id, ADMIN))
        .thenThrow(new RecursoNaoEncontradoException("Cartão não encontrado"));

    mockMvc.perform(get("/api/v1/cartoes/{id}", id).with(admin())).andExpect(status().isNotFound());
  }

  @Test
  void deveRetornar503ComRetryAfterQuandoProdutoIndisponivel() throws Exception {
    UUID id = UUID.randomUUID();
    when(buscarCartaoUseCase.executar(id, ADMIN))
        .thenThrow(
            new DependenciaIndisponivelException(
                "Produto Service indisponível", Duration.ofSeconds(10)));

    mockMvc
        .perform(get("/api/v1/cartoes/{id}", id).with(admin()))
        .andExpect(status().isServiceUnavailable())
        .andExpect(header().string("Retry-After", "10"));
  }

  @Test
  void deveListarCartoesDoPortadorPaginado() throws Exception {
    UUID portadorId = UUID.randomUUID();
    UUID produtoId = UUID.randomUUID();
    Cartao cartao = cartao(portadorId, produtoId);
    ProdutoDto produto =
        new ProdutoDto(produtoId, "Gold", "GOLD", "453201", StatusProdutoExterno.ATIVO);
    Pageable pageable = PageRequest.of(0, 20);
    when(listarCartoesPorPortadorUseCase.executar(eq(portadorId), any(Pageable.class), eq(ADMIN)))
        .thenReturn(
            new PageImpl<>(
                List.of(new CartaoComProduto(cartao, Optional.of(produto))), pageable, 1));

    mockMvc
        .perform(get("/api/v1/cartoes").param("portadorId", portadorId.toString()).with(admin()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.conteudo[0].id").value(cartao.getId().toString()))
        .andExpect(jsonPath("$.totalElementos").value(1));
  }

  @Test
  void deveAlterarStatusERetornarCartaoAtualizado() throws Exception {
    Cartao cartao = cartao(UUID.randomUUID(), UUID.randomUUID());
    cartao.bloquear(AGORA);
    when(alterarStatusCartaoUseCase.executar(cartao.getId(), StatusCartao.BLOQUEADO, ADMIN))
        .thenReturn(cartao);

    mockMvc
        .perform(
            patch("/api/v1/cartoes/{id}/status", cartao.getId())
                .with(admin())
                .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"status":"BLOQUEADO"}
                    """))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("BLOQUEADO"));
  }

  @Test
  void deveRetornar422QuandoTransicaoDeStatusInvalida() throws Exception {
    UUID id = UUID.randomUUID();
    when(alterarStatusCartaoUseCase.executar(id, StatusCartao.ATIVO, ADMIN))
        .thenThrow(new RegraNegocioException("Cartão já está ativo"));

    mockMvc
        .perform(
            patch("/api/v1/cartoes/{id}/status", id)
                .with(admin())
                .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"status":"ATIVO"}
                    """))
        .andExpect(status().isUnprocessableEntity());
  }

  @Test
  void deveRetornar409QuandoEscritaConcorrenteCausaOptimisticLock() throws Exception {
    UUID id = UUID.randomUUID();
    when(alterarStatusCartaoUseCase.executar(id, StatusCartao.CANCELADO, ADMIN))
        .thenThrow(new ObjectOptimisticLockingFailureException(Cartao.class, id));

    mockMvc
        .perform(
            patch("/api/v1/cartoes/{id}/status", id)
                .with(admin())
                .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"status":"CANCELADO"}
                    """))
        .andExpect(status().isConflict());
  }

  @Test
  void deveRetornar401QuandoSemToken() throws Exception {
    mockMvc
        .perform(get("/api/v1/cartoes/{id}", UUID.randomUUID()))
        .andExpect(status().isUnauthorized());
  }

  @Test
  void deveRetornar404NaAlteracaoDeStatusQuandoSolicitanteNaoEDono() throws Exception {
    UUID id = UUID.randomUUID();
    when(alterarStatusCartaoUseCase.executar(id, StatusCartao.CANCELADO, ADMIN))
        .thenThrow(new RecursoNaoEncontradoException("Cartão não encontrado"));

    mockMvc
        .perform(
            patch("/api/v1/cartoes/{id}/status", id)
                .with(admin())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"status\":\"CANCELADO\"}"))
        .andExpect(status().isNotFound());
  }

  @Test
  void deveEntregarTokenDeServicoAoCasoDeUsoComoSolicitanteDeServico() throws Exception {
    UUID portadorId = UUID.randomUUID();
    Pageable pageable = PageRequest.of(0, 1);
    Solicitante servico = Solicitante.deServico("portador-service");
    when(listarCartoesPorPortadorUseCase.executar(eq(portadorId), any(Pageable.class), eq(servico)))
        .thenReturn(new PageImpl<>(List.of(), pageable, 0));

    mockMvc
        .perform(
            get("/api/v1/cartoes")
                .param("portadorId", portadorId.toString())
                .with(
                    jwt()
                        .jwt(token -> token.subject("portador-service").claim("scope", "servico"))))
        .andExpect(status().isOk());
  }

  // Idioma inglês no pedido de propósito: a resposta é sempre em português (issue #119).
  @Test
  void deveResponderEmPortuguesQuandoOStatusForOmitido() throws Exception {
    mockMvc
        .perform(
            patch("/api/v1/cartoes/{id}/status", UUID.randomUUID())
                .with(admin())
                .header("Accept-Language", "en-US")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.errors[?(@.field=='status')].message").value("é obrigatório"));
  }

  @Test
  void deveResponderEmPortuguesQuandoOPortadorIdForOmitidoNaListagem() throws Exception {
    mockMvc
        .perform(get("/api/v1/cartoes").with(admin()).header("Accept-Language", "en-US"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.title").value("Parâmetro obrigatório ausente"))
        .andExpect(
            jsonPath("$.detail").value("O parâmetro obrigatório 'portadorId' não foi informado"));
  }

  @Test
  void deveResponderEmPortuguesQuandoOPortadorIdNaoForUmUuid() throws Exception {
    mockMvc
        .perform(
            get("/api/v1/cartoes")
                .param("portadorId", "abc")
                .with(admin())
                .header("Accept-Language", "en-US"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.title").value("Parâmetro inválido"))
        .andExpect(
            jsonPath("$.detail").value("O valor 'abc' não é válido para o parâmetro 'portadorId'"));
  }
}

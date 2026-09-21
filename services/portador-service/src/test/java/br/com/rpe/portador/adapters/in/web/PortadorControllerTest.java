package br.com.rpe.portador.adapters.in.web;

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

import br.com.rpe.portador.adapters.in.web.mapper.PortadorCompletoWebMapper;
import br.com.rpe.portador.adapters.in.web.mapper.PortadorWebMapperImpl;
import br.com.rpe.portador.adapters.in.web.security.JwtAccessDeniedHandler;
import br.com.rpe.portador.adapters.in.web.security.JwtAuthEntryPoint;
import br.com.rpe.portador.application.port.out.CartaoDto;
import br.com.rpe.portador.application.port.out.FalhaEmissaoDto;
import br.com.rpe.portador.application.port.out.ProdutoDto;
import br.com.rpe.portador.application.port.out.StatusCartaoExterno;
import br.com.rpe.portador.application.port.out.StatusProdutoExterno;
import br.com.rpe.portador.application.seguranca.Solicitante;
import br.com.rpe.portador.application.usecase.AlterarStatusPortadorUseCase;
import br.com.rpe.portador.application.usecase.BuscarPortadorCompletoUseCase;
import br.com.rpe.portador.application.usecase.BuscarPortadorUseCase;
import br.com.rpe.portador.application.usecase.CadastrarPortadorUseCase;
import br.com.rpe.portador.application.usecase.PortadorCompleto;
import br.com.rpe.portador.application.usecase.PortadorCompleto.StatusEmissao;
import br.com.rpe.portador.config.ClockConfig;
import br.com.rpe.portador.config.SecurityConfig;
import br.com.rpe.portador.domain.Cpf;
import br.com.rpe.portador.domain.Portador;
import br.com.rpe.portador.domain.StatusPortador;
import br.com.rpe.portador.domain.exception.ConflitoException;
import br.com.rpe.portador.domain.exception.DependenciaIndisponivelException;
import br.com.rpe.portador.domain.exception.RecursoNaoEncontradoException;
import br.com.rpe.portador.domain.exception.RegraNegocioException;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.MediaType;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.JwtRequestPostProcessor;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(PortadorController.class)
@Import({
  PortadorWebMapperImpl.class,
  PortadorCompletoWebMapper.class,
  ClockConfig.class,
  SecurityConfig.class,
  ProblemDetailFactory.class,
  GlobalExceptionHandler.class,
  JwtAuthEntryPoint.class,
  JwtAccessDeniedHandler.class
})
@TestPropertySource(
    properties = {
      "rpe.jwt.secret=segredo-de-teste-com-pelo-menos-32-caracteres",
      "rpe.jwt.issuer=https://portador-service.rpe.local",
      "rpe.jwt.audience=rpe-api",
      "rpe.jwt.expiracao=30m",
      "rpe.auth.usuario-seed.username=admin",
      "rpe.auth.usuario-seed.password-hash=hash-qualquer"
    })
class PortadorControllerTest {

  private static final Instant AGORA = Instant.parse("2026-09-19T12:00:00Z");
  private static final String CPF_VALIDO = "529.982.247-25";
  private static final UUID PRODUTO_ID = UUID.randomUUID();
  private static final Solicitante ADMIN = Solicitante.deUsuario("admin");

  @Autowired private MockMvc mockMvc;

  @MockitoBean private CadastrarPortadorUseCase cadastrarPortadorUseCase;
  @MockitoBean private BuscarPortadorUseCase buscarPortadorUseCase;
  @MockitoBean private BuscarPortadorCompletoUseCase buscarPortadorCompletoUseCase;
  @MockitoBean private AlterarStatusPortadorUseCase alterarStatusPortadorUseCase;

  // JWT de usuário: o sub vira o Solicitante que o controller entrega ao caso de uso (ADR-009).
  private static JwtRequestPostProcessor admin() {
    return jwt().jwt(token -> token.subject("admin"));
  }

  private String corpo(String cpf, String dataNascimento, String produtoId) {
    return """
        {"nome":"Victor Rodrigues","cpf":"%s","dataNascimento":"%s","produtoId":"%s"}
        """
        .formatted(cpf, dataNascimento, produtoId);
  }

  @Test
  void deveCadastrarERetornar201ComLocationECpfMascarado() throws Exception {
    Portador portador =
        Portador.cadastrar(
            "Victor Rodrigues",
            Cpf.of(CPF_VALIDO),
            LocalDate.of(2000, 1, 1),
            PRODUTO_ID,
            "admin",
            AGORA);
    when(cadastrarPortadorUseCase.executar(
            eq("Victor Rodrigues"),
            eq(Cpf.of(CPF_VALIDO)),
            eq(LocalDate.of(2000, 1, 1)),
            eq(PRODUTO_ID),
            eq(ADMIN),
            any()))
        .thenReturn(portador);

    mockMvc
        .perform(
            post("/api/v1/portadores")
                .with(admin())
                .contentType(MediaType.APPLICATION_JSON)
                .content(corpo(CPF_VALIDO, "2000-01-01", PRODUTO_ID.toString())))
        .andExpect(status().isCreated())
        .andExpect(header().string("Location", "/api/v1/portadores/" + portador.getId()))
        .andExpect(jsonPath("$.cpf").value("***.982.247-**"));
  }

  @Test
  void deveRetornar401QuandoSemToken() throws Exception {
    mockMvc
        .perform(
            post("/api/v1/portadores")
                .contentType(MediaType.APPLICATION_JSON)
                .content(corpo(CPF_VALIDO, "2000-01-01", PRODUTO_ID.toString())))
        .andExpect(status().isUnauthorized());
  }

  @Test
  void deveRetornar400QuandoCpfComFormatoInvalido() throws Exception {
    mockMvc
        .perform(
            post("/api/v1/portadores")
                .with(admin())
                .contentType(MediaType.APPLICATION_JSON)
                .content(corpo("123", "2000-01-01", PRODUTO_ID.toString())))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.errors[0].field").value("cpf"));
  }

  @Test
  void deveRetornar422QuandoProdutoNaoEstaAtivo() throws Exception {
    when(cadastrarPortadorUseCase.executar(any(), any(), any(), any(), any(), any()))
        .thenThrow(new RegraNegocioException("Produto não está ATIVO"));

    mockMvc
        .perform(
            post("/api/v1/portadores")
                .with(admin())
                .contentType(MediaType.APPLICATION_JSON)
                .content(corpo(CPF_VALIDO, "2000-01-01", PRODUTO_ID.toString())))
        .andExpect(status().isUnprocessableEntity());
  }

  @Test
  void deveRetornar422QuandoMenorDeIdade() throws Exception {
    when(cadastrarPortadorUseCase.executar(any(), any(), any(), any(), any(), any()))
        .thenThrow(new RegraNegocioException("Portador deve ter pelo menos 18 anos completos"));

    mockMvc
        .perform(
            post("/api/v1/portadores")
                .with(admin())
                .contentType(MediaType.APPLICATION_JSON)
                .content(corpo(CPF_VALIDO, "2015-01-01", PRODUTO_ID.toString())))
        .andExpect(status().isUnprocessableEntity());
  }

  @Test
  void deveRetornar409QuandoCpfDuplicado() throws Exception {
    when(cadastrarPortadorUseCase.executar(any(), any(), any(), any(), any(), any()))
        .thenThrow(new ConflitoException("CPF já cadastrado"));

    mockMvc
        .perform(
            post("/api/v1/portadores")
                .with(admin())
                .contentType(MediaType.APPLICATION_JSON)
                .content(corpo(CPF_VALIDO, "2000-01-01", PRODUTO_ID.toString())))
        .andExpect(status().isConflict());
  }

  @Test
  void deveRetornar409QuandoConstraintDeUnicidadeViolarNaCorrida() throws Exception {
    when(cadastrarPortadorUseCase.executar(any(), any(), any(), any(), any(), any()))
        .thenThrow(new DataIntegrityViolationException("uk_portador_cpf"));

    mockMvc
        .perform(
            post("/api/v1/portadores")
                .with(admin())
                .contentType(MediaType.APPLICATION_JSON)
                .content(corpo(CPF_VALIDO, "2000-01-01", PRODUTO_ID.toString())))
        .andExpect(status().isConflict());
  }

  @Test
  void deveRetornar503ComRetryAfterQuandoProdutoIndisponivel() throws Exception {
    when(cadastrarPortadorUseCase.executar(any(), any(), any(), any(), any(), any()))
        .thenThrow(
            new DependenciaIndisponivelException(
                "Produto Service indisponível", Duration.ofSeconds(10)));

    mockMvc
        .perform(
            post("/api/v1/portadores")
                .with(admin())
                .contentType(MediaType.APPLICATION_JSON)
                .content(corpo(CPF_VALIDO, "2000-01-01", PRODUTO_ID.toString())))
        .andExpect(status().isServiceUnavailable())
        .andExpect(header().string("Retry-After", "10"));
  }

  @Test
  void deveBuscarPortadorPorIdComCpfMascarado() throws Exception {
    Portador portador =
        Portador.cadastrar(
            "Victor Rodrigues",
            Cpf.of(CPF_VALIDO),
            LocalDate.of(2000, 1, 1),
            PRODUTO_ID,
            "admin",
            AGORA);
    when(buscarPortadorUseCase.executar(portador.getId(), ADMIN)).thenReturn(portador);

    mockMvc
        .perform(get("/api/v1/portadores/{id}", portador.getId()).with(admin()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(portador.getId().toString()))
        .andExpect(jsonPath("$.cpf").value("***.982.247-**"));
  }

  @Test
  void deveRetornar404QuandoPortadorNaoEncontrado() throws Exception {
    UUID id = UUID.randomUUID();
    when(buscarPortadorUseCase.executar(id, ADMIN))
        .thenThrow(new RecursoNaoEncontradoException("Portador não encontrado"));

    mockMvc
        .perform(get("/api/v1/portadores/{id}", id).with(admin()))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.title").value("Recurso não encontrado"));
  }

  @Test
  void deveAlterarStatusERetornarPortadorAtualizado() throws Exception {
    Portador portador =
        Portador.cadastrar(
            "Victor Rodrigues",
            Cpf.of(CPF_VALIDO),
            LocalDate.of(2000, 1, 1),
            PRODUTO_ID,
            "admin",
            AGORA);
    portador.bloquear(AGORA);
    when(alterarStatusPortadorUseCase.executar(portador.getId(), StatusPortador.BLOQUEADO, ADMIN))
        .thenReturn(portador);

    mockMvc
        .perform(
            patch("/api/v1/portadores/{id}/status", portador.getId())
                .with(admin())
                .contentType(MediaType.APPLICATION_JSON)
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
    when(alterarStatusPortadorUseCase.executar(id, StatusPortador.ATIVO, ADMIN))
        .thenThrow(new RegraNegocioException("Portador já está ativo"));

    mockMvc
        .perform(
            patch("/api/v1/portadores/{id}/status", id)
                .with(admin())
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"status":"ATIVO"}
                    """))
        .andExpect(status().isUnprocessableEntity());
  }

  @Test
  void deveRetornar409QuandoEscritaConcorrenteCausaOptimisticLock() throws Exception {
    UUID id = UUID.randomUUID();
    when(alterarStatusPortadorUseCase.executar(id, StatusPortador.CANCELADO, ADMIN))
        .thenThrow(new ObjectOptimisticLockingFailureException(Portador.class, id));

    mockMvc
        .perform(
            patch("/api/v1/portadores/{id}/status", id)
                .with(admin())
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"status":"CANCELADO"}
                    """))
        .andExpect(status().isConflict());
  }

  @Test
  void deveBuscarCompletoComCartaoEProduto() throws Exception {
    Portador portador =
        Portador.cadastrar(
            "Victor Rodrigues",
            Cpf.of(CPF_VALIDO),
            LocalDate.of(2000, 1, 1),
            PRODUTO_ID,
            "admin",
            AGORA);
    CartaoDto cartao =
        new CartaoDto(UUID.randomUUID(), "**** **** **** 1234", "09/31", StatusCartaoExterno.ATIVO);
    ProdutoDto produto = new ProdutoDto(PRODUTO_ID, "Gold", "GOLD", StatusProdutoExterno.ATIVO);
    when(buscarPortadorCompletoUseCase.executar(portador.getId(), ADMIN))
        .thenReturn(
            new PortadorCompleto(
                portador,
                Optional.of(cartao),
                Optional.of(produto),
                Optional.empty(),
                StatusEmissao.CONCLUIDA,
                List.of()));

    mockMvc
        .perform(get("/api/v1/portadores/{id}/completo", portador.getId()).with(admin()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.portador.cpf").value("***.982.247-**"))
        .andExpect(jsonPath("$.cartao.panMascarado").value("**** **** **** 1234"))
        .andExpect(jsonPath("$.produto.nome").value("Gold"))
        .andExpect(jsonPath("$.emissao").value("CONCLUIDA"))
        .andExpect(jsonPath("$.avisos").isEmpty());
  }

  @Test
  void deveRetornarDegradadoComAvisoQuandoCartaoIndisponivel() throws Exception {
    Portador portador =
        Portador.cadastrar(
            "Victor Rodrigues",
            Cpf.of(CPF_VALIDO),
            LocalDate.of(2000, 1, 1),
            PRODUTO_ID,
            "admin",
            AGORA);
    ProdutoDto produto = new ProdutoDto(PRODUTO_ID, "Gold", "GOLD", StatusProdutoExterno.ATIVO);
    when(buscarPortadorCompletoUseCase.executar(portador.getId(), ADMIN))
        .thenReturn(
            new PortadorCompleto(
                portador,
                Optional.empty(),
                Optional.of(produto),
                Optional.empty(),
                StatusEmissao.DESCONHECIDA,
                List.of("Cartão indisponível no momento")));

    mockMvc
        .perform(get("/api/v1/portadores/{id}/completo", portador.getId()).with(admin()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.cartao").doesNotExist())
        .andExpect(jsonPath("$.emissao").value("DESCONHECIDA"))
        .andExpect(jsonPath("$.avisos[0]").value("Cartão indisponível no momento"));
  }

  @Test
  void deveRetornar404NaConsultaCompletaQuandoPortadorNaoEncontrado() throws Exception {
    UUID id = UUID.randomUUID();
    when(buscarPortadorCompletoUseCase.executar(id, ADMIN))
        .thenThrow(new RecursoNaoEncontradoException("Portador não encontrado"));

    mockMvc
        .perform(get("/api/v1/portadores/{id}/completo", id).with(admin()))
        .andExpect(status().isNotFound());
  }

  @Test
  void deveRetornar404NaAlteracaoDeStatusQuandoSolicitanteNaoEDono() throws Exception {
    UUID id = UUID.randomUUID();
    when(alterarStatusPortadorUseCase.executar(id, StatusPortador.CANCELADO, ADMIN))
        .thenThrow(new RecursoNaoEncontradoException("Portador não encontrado"));

    mockMvc
        .perform(
            patch("/api/v1/portadores/{id}/status", id)
                .with(admin())
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"status":"CANCELADO"}
                    """))
        .andExpect(status().isNotFound());
  }

  @Test
  void deveEntregarTokenDeServicoAoCasoDeUsoComoSolicitanteDeServico() throws Exception {
    Portador portador =
        Portador.cadastrar(
            "Victor Rodrigues",
            Cpf.of(CPF_VALIDO),
            LocalDate.of(2000, 1, 1),
            PRODUTO_ID,
            "admin",
            AGORA);
    when(buscarPortadorUseCase.executar(portador.getId(), Solicitante.deServico("cartao-service")))
        .thenReturn(portador);

    mockMvc
        .perform(
            get("/api/v1/portadores/{id}", portador.getId())
                .with(
                    jwt().jwt(token -> token.subject("cartao-service").claim("scope", "servico"))))
        .andExpect(status().isOk());
  }

  @Test
  void deveRetornarEmissaoFalhouComMotivoEHorario() throws Exception {
    Portador portador =
        Portador.cadastrar(
            "Victor Rodrigues",
            Cpf.of(CPF_VALIDO),
            LocalDate.of(2000, 1, 1),
            PRODUTO_ID,
            "admin",
            AGORA);
    when(buscarPortadorCompletoUseCase.executar(portador.getId(), ADMIN))
        .thenReturn(
            new PortadorCompleto(
                portador,
                Optional.empty(),
                Optional.empty(),
                Optional.of(new FalhaEmissaoDto("Produto inexistente ou não ATIVO", AGORA)),
                StatusEmissao.FALHOU,
                List.of()));

    mockMvc
        .perform(get("/api/v1/portadores/{id}/completo", portador.getId()).with(admin()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.emissao").value("FALHOU"))
        .andExpect(jsonPath("$.cartao").doesNotExist())
        .andExpect(jsonPath("$.falhaEmissao.motivo").value("Produto inexistente ou não ATIVO"))
        .andExpect(jsonPath("$.falhaEmissao.ocorridaEm").value("2026-09-19T12:00:00Z"));
  }

  // Idioma inglês no pedido de propósito: a resposta é sempre em português (issue #119).
  @Test
  void deveResponderOsErrosDeCampoEmPortuguesMesmoComIdiomaIngles() throws Exception {
    mockMvc
        .perform(
            post("/api/v1/portadores")
                .with(admin())
                .header("Accept-Language", "en-US")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
        .andExpect(status().isBadRequest())
        .andExpect(
            jsonPath("$.errors[?(@.field=='nome')].message").value("não pode estar em branco"))
        .andExpect(
            jsonPath("$.errors[?(@.field=='cpf')].message").value("não pode estar em branco"))
        .andExpect(
            jsonPath("$.errors[?(@.field=='dataNascimento')].message").value("é obrigatório"))
        .andExpect(jsonPath("$.errors[?(@.field=='produtoId')].message").value("é obrigatório"));
  }

  @Test
  void deveResponderEmPortuguesQuandoADataDeNascimentoNaoEPassada() throws Exception {
    mockMvc
        .perform(
            post("/api/v1/portadores")
                .with(admin())
                .header("Accept-Language", "en-US")
                .contentType(MediaType.APPLICATION_JSON)
                .content(corpo(CPF_VALIDO, "2999-01-01", PRODUTO_ID.toString())))
        .andExpect(status().isBadRequest())
        .andExpect(
            jsonPath("$.errors[?(@.field=='dataNascimento')].message")
                .value("deve ser uma data no passado"));
  }

  @Test
  void deveResponderEmPortuguesQuandoOCorpoForIlegivel() throws Exception {
    mockMvc
        .perform(
            post("/api/v1/portadores")
                .with(admin())
                .header("Accept-Language", "en-US")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{ nome"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.title").value("Requisição ilegível"))
        .andExpect(
            jsonPath("$.detail")
                .value(org.hamcrest.Matchers.containsString("corpo da requisição")));
  }

  @Test
  void deveResponderEmPortuguesQuandoOIdentificadorNaoForUmUuid() throws Exception {
    mockMvc
        .perform(
            get("/api/v1/portadores/{id}", "nao-e-uuid")
                .with(admin())
                .header("Accept-Language", "en-US"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.title").value("Parâmetro inválido"))
        .andExpect(
            jsonPath("$.detail").value("O valor 'nao-e-uuid' não é válido para o parâmetro 'id'"));
  }

  @Test
  void deveResponderEmPortuguesQuandoOMetodoNaoForSuportado() throws Exception {
    mockMvc
        .perform(
            org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete(
                    "/api/v1/portadores/{id}", UUID.randomUUID())
                .with(admin())
                .header("Accept-Language", "en-US"))
        .andExpect(status().isMethodNotAllowed())
        .andExpect(jsonPath("$.title").value("Método não permitido"))
        .andExpect(jsonPath("$.detail").value("O método 'DELETE' não é suportado neste endereço"));
  }

  @Test
  void deveResponderEmPortuguesQuandoOTipoDeConteudoNaoForSuportado() throws Exception {
    mockMvc
        .perform(
            post("/api/v1/portadores")
                .with(admin())
                .header("Accept-Language", "en-US")
                .content("{}"))
        .andExpect(status().isUnsupportedMediaType())
        .andExpect(jsonPath("$.title").value("Tipo de conteúdo não suportado"));
  }
}

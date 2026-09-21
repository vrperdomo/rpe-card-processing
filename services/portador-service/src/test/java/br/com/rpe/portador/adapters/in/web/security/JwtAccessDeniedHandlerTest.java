package br.com.rpe.portador.adapters.in.web.security;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.rpe.portador.adapters.in.web.ProblemDetailFactory;
import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.access.AccessDeniedException;

class JwtAccessDeniedHandlerTest {

  private final Logger logger = (Logger) LoggerFactory.getLogger(JwtAccessDeniedHandler.class);
  private final ListAppender<ILoggingEvent> logs = new ListAppender<>();

  private JwtAccessDeniedHandler handler;

  @BeforeEach
  void configurar() {
    logs.start();
    logger.addAppender(logs);
    Clock clock = Clock.fixed(Instant.parse("2026-09-21T10:00:00Z"), ZoneOffset.UTC);
    handler =
        new JwtAccessDeniedHandler(
            new ProblemDetailFactory(clock), new ObjectMapper().findAndRegisterModules());
  }

  @AfterEach
  void limpar() {
    logger.detachAppender(logs);
  }

  @Test
  void deveLogarWarnComMetodoCaminhoOrigemEUsuarioQuandoAcessoNegado() throws Exception {
    var request = new MockHttpServletRequest("PATCH", "/api/v1/portadores/123/status");
    request.setRemoteAddr("203.0.113.7");
    request.setUserPrincipal(() -> "admin");

    handler.handle(request, new MockHttpServletResponse(), new AccessDeniedException("negado"));

    assertThat(logs.list).hasSize(1);
    ILoggingEvent evento = logs.list.get(0);
    assertThat(evento.getLevel()).isEqualTo(Level.WARN);
    assertThat(evento.getFormattedMessage())
        .contains("metodo=PATCH")
        .contains("caminho=/api/v1/portadores/123/status")
        .contains("origem=203.0.113.7")
        .contains("usuario=admin");
  }

  @Test
  void deveLogarUsuarioDesconhecidoQuandoNaoHaPrincipal() throws Exception {
    var request = new MockHttpServletRequest("GET", "/api/v1/portadores/123");

    handler.handle(request, new MockHttpServletResponse(), new AccessDeniedException("negado"));

    assertThat(logs.list).hasSize(1);
    assertThat(logs.list.get(0).getFormattedMessage()).contains("usuario=desconhecido");
  }

  @Test
  void naoDeveLogarTokenNemQueryString() throws Exception {
    var request = new MockHttpServletRequest("GET", "/api/v1/portadores/123");
    request.addHeader("Authorization", "Bearer eyJ.segredo.assinatura");
    request.setQueryString("cpf=12345678909");

    handler.handle(request, new MockHttpServletResponse(), new AccessDeniedException("negado"));

    assertThat(logs.list.get(0).getFormattedMessage())
        .doesNotContain("eyJ.segredo.assinatura")
        .doesNotContain("12345678909");
  }

  @Test
  void deveResponder403ComProblemDetail() throws Exception {
    var response = new MockHttpServletResponse();

    handler.handle(
        new MockHttpServletRequest("GET", "/api/v1/portadores/123"),
        response,
        new AccessDeniedException("negado"));

    assertThat(response.getStatus()).isEqualTo(403);
    assertThat(response.getContentType()).isEqualTo(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
    assertThat(response.getContentAsString()).contains("Sem permissão");
  }
}

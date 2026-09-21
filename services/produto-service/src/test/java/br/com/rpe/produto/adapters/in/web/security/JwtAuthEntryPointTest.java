package br.com.rpe.produto.adapters.in.web.security;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.rpe.produto.adapters.in.web.ProblemDetailFactory;
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
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.InsufficientAuthenticationException;

class JwtAuthEntryPointTest {

  private final Logger logger = (Logger) LoggerFactory.getLogger(JwtAuthEntryPoint.class);
  private final ListAppender<ILoggingEvent> logs = new ListAppender<>();

  private JwtAuthEntryPoint entryPoint;

  @BeforeEach
  void configurar() {
    logs.start();
    logger.addAppender(logs);
    Clock clock = Clock.fixed(Instant.parse("2026-09-21T10:00:00Z"), ZoneOffset.UTC);
    entryPoint =
        new JwtAuthEntryPoint(
            new ProblemDetailFactory(clock), new ObjectMapper().findAndRegisterModules());
  }

  @AfterEach
  void limpar() {
    logger.detachAppender(logs);
  }

  @Test
  void deveLogarWarnComMetodoCaminhoOrigemEMotivoQuandoNaoAutenticado() throws Exception {
    var request = new MockHttpServletRequest("GET", "/api/v1/produtos/123");
    request.setRemoteAddr("203.0.113.7");

    entryPoint.commence(
        request, new MockHttpServletResponse(), new InsufficientAuthenticationException("x"));

    assertThat(logs.list).hasSize(1);
    ILoggingEvent evento = logs.list.get(0);
    assertThat(evento.getLevel()).isEqualTo(Level.WARN);
    assertThat(evento.getFormattedMessage())
        .contains("metodo=GET")
        .contains("caminho=/api/v1/produtos/123")
        .contains("origem=203.0.113.7")
        .contains("motivo=InsufficientAuthenticationException");
  }

  @Test
  void naoDeveLogarTokenQueryStringNemMensagemDaExcecao() throws Exception {
    var request = new MockHttpServletRequest("GET", "/api/v1/produtos/123");
    request.addHeader("Authorization", "Bearer eyJ.segredo.assinatura");
    request.setQueryString("cpf=12345678909");

    entryPoint.commence(
        request,
        new MockHttpServletResponse(),
        new BadCredentialsException("token eyJ.segredo.assinatura rejeitado"));

    assertThat(logs.list).hasSize(1);
    assertThat(logs.list.get(0).getFormattedMessage())
        .contains("motivo=BadCredentialsException")
        .doesNotContain("eyJ.segredo.assinatura")
        .doesNotContain("12345678909")
        .doesNotContain("rejeitado");
  }

  @Test
  void deveResponder401ComProblemDetail() throws Exception {
    var response = new MockHttpServletResponse();

    entryPoint.commence(
        new MockHttpServletRequest("GET", "/api/v1/produtos/123"),
        response,
        new InsufficientAuthenticationException("x"));

    assertThat(response.getStatus()).isEqualTo(401);
    assertThat(response.getContentType()).isEqualTo(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
    assertThat(response.getContentAsString()).contains("Não autenticado");
  }
}

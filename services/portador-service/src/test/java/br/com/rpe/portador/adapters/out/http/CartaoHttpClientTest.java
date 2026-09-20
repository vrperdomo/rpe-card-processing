package br.com.rpe.portador.adapters.out.http;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.matching;
import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.com.rpe.portador.IntegrationTestBase;
import br.com.rpe.portador.application.port.out.CartaoDto;
import br.com.rpe.portador.application.port.out.StatusCartaoExterno;
import br.com.rpe.portador.domain.exception.DependenciaIndisponivelException;
import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import java.time.Duration;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.NONE,
    properties = "rpe.portador.outbox.relay.ativo=false")
class CartaoHttpClientTest extends IntegrationTestBase {

  @RegisterExtension
  static WireMockExtension wireMock =
      WireMockExtension.newInstance().options(wireMockConfig().dynamicPort()).build();

  @Autowired private CartaoHttpClient cartaoClient;
  @Autowired private CircuitBreakerRegistry circuitBreakerRegistry;

  @DynamicPropertySource
  static void propriedades(DynamicPropertyRegistry registry) {
    registry.add("rpe.portador.cartao-client.base-url", wireMock::baseUrl);
    registry.add("resilience4j.retry.instances.cartao.max-attempts", () -> "3");
    registry.add("resilience4j.retry.instances.cartao.wait-duration", () -> "20ms");
    registry.add("resilience4j.circuitbreaker.instances.cartao.sliding-window-size", () -> "20");
    registry.add(
        "resilience4j.circuitbreaker.instances.cartao.minimum-number-of-calls", () -> "20");
    registry.add(
        "resilience4j.circuitbreaker.instances.cartao.wait-duration-in-open-state", () -> "10s");
    registry.add("resilience4j.timelimiter.instances.cartao.timeout-duration", () -> "2s");
  }

  @BeforeEach
  void resetarCircuitBreaker() {
    circuitBreakerRegistry.circuitBreaker("cartao").reset();
  }

  private String caminho() {
    return "/api/v1/cartoes";
  }

  @Test
  void deveRetornarCartaoQuandoEncontrado() {
    UUID portadorId = UUID.randomUUID();
    UUID cartaoId = UUID.randomUUID();
    wireMock.stubFor(
        get(urlPathEqualTo(caminho()))
            .willReturn(
                okJson(
                    """
                    {"conteudo":[{"id":"%s","panMascarado":"**** **** **** 1234","validade":"09/31","status":"ATIVO"}],"pagina":0,"tamanho":1,"totalElementos":1,"totalPaginas":1}
                    """
                        .formatted(cartaoId))));

    Optional<CartaoDto> resultado = cartaoClient.buscarPorPortadorId(portadorId);

    assertThat(resultado)
        .contains(
            new CartaoDto(cartaoId, "**** **** **** 1234", "09/31", StatusCartaoExterno.ATIVO));
    wireMock.verify(
        getRequestedFor(urlPathEqualTo(caminho()))
            .withHeader("Authorization", matching("Bearer .+")));
  }

  @Test
  void deveRetornarVazioQuandoNenhumCartaoParaOPortador() {
    UUID portadorId = UUID.randomUUID();
    wireMock.stubFor(
        get(urlPathEqualTo(caminho()))
            .willReturn(
                okJson(
                    """
                    {"conteudo":[],"pagina":0,"tamanho":1,"totalElementos":0,"totalPaginas":0}
                    """)));

    Optional<CartaoDto> resultado = cartaoClient.buscarPorPortadorId(portadorId);

    assertThat(resultado).isEmpty();
    wireMock.verify(1, getRequestedFor(urlPathEqualTo(caminho())));
  }

  @Test
  void deveLancarDependenciaIndisponivelQuandoErroPersistente() {
    UUID portadorId = UUID.randomUUID();
    wireMock.stubFor(get(urlPathEqualTo(caminho())).willReturn(aResponse().withStatus(500)));

    assertThatThrownBy(() -> cartaoClient.buscarPorPortadorId(portadorId))
        .isInstanceOfSatisfying(
            DependenciaIndisponivelException.class,
            ex -> assertThat(ex.getRetryAfter()).isEqualTo(Duration.ofSeconds(10)));
    wireMock.verify(3, getRequestedFor(urlPathEqualTo(caminho())));
  }

  @Test
  void deveAbrirCircuitoAposFalhasConsecutivas() {
    wireMock.stubFor(get(urlPathEqualTo(caminho())).willReturn(aResponse().withStatus(500)));

    for (int i = 0; i < 7; i++) {
      UUID portadorId = UUID.randomUUID();
      assertThatThrownBy(() -> cartaoClient.buscarPorPortadorId(portadorId))
          .isInstanceOf(DependenciaIndisponivelException.class);
    }

    CircuitBreaker circuitBreaker = circuitBreakerRegistry.circuitBreaker("cartao");
    assertThat(circuitBreaker.getState()).isEqualTo(CircuitBreaker.State.OPEN);
  }
}

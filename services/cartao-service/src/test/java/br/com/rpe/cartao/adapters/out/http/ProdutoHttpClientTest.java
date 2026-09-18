package br.com.rpe.cartao.adapters.out.http;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.matching;
import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.com.rpe.cartao.application.port.out.ProdutoClient;
import br.com.rpe.cartao.application.port.out.ProdutoDto;
import br.com.rpe.cartao.application.port.out.StatusProdutoExterno;
import br.com.rpe.cartao.domain.exception.DependenciaIndisponivelException;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import com.github.tomakehurst.wiremock.stubbing.Scenario;
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

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
class ProdutoHttpClientTest {

  @RegisterExtension
  static WireMockExtension wireMock =
      WireMockExtension.newInstance().options(wireMockConfig().dynamicPort()).build();

  @Autowired private ProdutoClient produtoClient;
  @Autowired private CircuitBreakerRegistry circuitBreakerRegistry;

  @DynamicPropertySource
  static void propriedades(DynamicPropertyRegistry registry) {
    registry.add("rpe.cartao.produto-client.base-url", wireMock::baseUrl);
    registry.add("resilience4j.retry.instances.produto.max-attempts", () -> "3");
    registry.add("resilience4j.retry.instances.produto.wait-duration", () -> "20ms");
    registry.add("resilience4j.circuitbreaker.instances.produto.sliding-window-size", () -> "20");
    registry.add(
        "resilience4j.circuitbreaker.instances.produto.minimum-number-of-calls", () -> "20");
    registry.add(
        "resilience4j.circuitbreaker.instances.produto.wait-duration-in-open-state", () -> "10s");
    registry.add("resilience4j.timelimiter.instances.produto.timeout-duration", () -> "2s");
  }

  @BeforeEach
  void resetarCircuitBreaker() {
    circuitBreakerRegistry.circuitBreaker("produto").reset();
  }

  private String caminho(UUID id) {
    return "/api/v1/produtos/" + id;
  }

  @Test
  void deveRetornarProdutoQuandoRespostaComSucesso() {
    UUID produtoId = UUID.randomUUID();
    wireMock.stubFor(
        get(urlEqualTo(caminho(produtoId)))
            .willReturn(
                okJson(
                    """
                    {"id":"%s","nome":"Gold","categoria":"GOLD","bin":"453201","status":"ATIVO"}
                    """
                        .formatted(produtoId))));

    Optional<ProdutoDto> resultado = produtoClient.buscarPorId(produtoId);

    assertThat(resultado)
        .contains(new ProdutoDto(produtoId, "Gold", "GOLD", "453201", StatusProdutoExterno.ATIVO));
    wireMock.verify(
        getRequestedFor(urlEqualTo(caminho(produtoId)))
            .withHeader("Authorization", matching("Bearer .+")));
  }

  @Test
  void deveRetornarVazioQuandoProdutoNaoEncontradoSemRetentativa() {
    UUID produtoId = UUID.randomUUID();
    wireMock.stubFor(get(urlEqualTo(caminho(produtoId))).willReturn(aResponse().withStatus(404)));

    Optional<ProdutoDto> resultado = produtoClient.buscarPorId(produtoId);

    assertThat(resultado).isEmpty();
    wireMock.verify(1, getRequestedFor(urlEqualTo(caminho(produtoId))));
  }

  @Test
  void deveReentarErroTransitorioAteSucesso() {
    UUID produtoId = UUID.randomUUID();
    String cenario = "recuperacao-produto";
    wireMock.stubFor(
        get(urlEqualTo(caminho(produtoId)))
            .inScenario(cenario)
            .whenScenarioStateIs(Scenario.STARTED)
            .willReturn(aResponse().withStatus(500))
            .willSetStateTo("segunda-tentativa"));
    wireMock.stubFor(
        get(urlEqualTo(caminho(produtoId)))
            .inScenario(cenario)
            .whenScenarioStateIs("segunda-tentativa")
            .willReturn(
                okJson(
                    """
                    {"id":"%s","nome":"Gold","categoria":"GOLD","bin":"453201","status":"ATIVO"}
                    """
                        .formatted(produtoId))));

    Optional<ProdutoDto> resultado = produtoClient.buscarPorId(produtoId);

    assertThat(resultado).isPresent();
    wireMock.verify(2, getRequestedFor(urlEqualTo(caminho(produtoId))));
  }

  @Test
  void deveLancarDependenciaIndisponivelQuandoErroPersistente() {
    UUID produtoId = UUID.randomUUID();
    wireMock.stubFor(get(urlEqualTo(caminho(produtoId))).willReturn(aResponse().withStatus(500)));

    assertThatThrownBy(() -> produtoClient.buscarPorId(produtoId))
        .isInstanceOfSatisfying(
            DependenciaIndisponivelException.class,
            ex -> assertThat(ex.getRetryAfter()).isEqualTo(Duration.ofSeconds(10)));
    wireMock.verify(3, getRequestedFor(urlEqualTo(caminho(produtoId))));
  }

  @Test
  void deveLancarDependenciaIndisponivelQuandoProdutoLento() {
    UUID produtoId = UUID.randomUUID();
    wireMock.stubFor(
        get(urlEqualTo(caminho(produtoId))).willReturn(okJson("{}").withFixedDelay(5000)));

    assertThatThrownBy(() -> produtoClient.buscarPorId(produtoId))
        .isInstanceOf(DependenciaIndisponivelException.class);
  }

  @Test
  void deveAbrirCircuitoAposFalhasConsecutivas() {
    wireMock.stubFor(WireMock.any(WireMock.anyUrl()).willReturn(aResponse().withStatus(500)));

    for (int i = 0; i < 7; i++) {
      UUID produtoId = UUID.randomUUID();
      assertThatThrownBy(() -> produtoClient.buscarPorId(produtoId))
          .isInstanceOf(DependenciaIndisponivelException.class);
    }

    CircuitBreaker circuitBreaker = circuitBreakerRegistry.circuitBreaker("produto");
    assertThat(circuitBreaker.getState()).isEqualTo(CircuitBreaker.State.OPEN);
  }
}

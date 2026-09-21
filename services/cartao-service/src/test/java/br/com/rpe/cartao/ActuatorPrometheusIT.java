package br.com.rpe.cartao;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.actuate.observability.AutoConfigureObservability;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

/**
 * O /actuator/prometheus existe de verdade (issue #131): estava listado em
 * management.endpoints.web.exposure.include, mas sem o registry do Micrometer no classpath o
 * endpoint respondia 404. Segue autenticado: só health é público (ADR-009, A05).
 * {@code @AutoConfigureObservability}: o Spring Boot desliga a exportação de métricas em testes.
 */
@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.MOCK,
    properties = "rpe.cartao.mensageria.listener-auto-startup=false")
@AutoConfigureMockMvc
@AutoConfigureObservability
class ActuatorPrometheusIT extends IntegrationTestBase {

  @Autowired private MockMvc mockMvc;

  @Test
  void deveExporMetricasNoFormatoPrometheusParaQuemTemToken() throws Exception {
    mockMvc
        .perform(get("/actuator/prometheus").with(jwt()))
        .andExpect(status().isOk())
        .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_PLAIN))
        .andExpect(content().string(Matchers.containsString("# TYPE jvm_memory_used_bytes gauge")))
        .andExpect(content().string(Matchers.containsString("process_uptime_seconds")));
  }

  @Test
  void naoDeveExporMetricasSemToken() throws Exception {
    mockMvc.perform(get("/actuator/prometheus")).andExpect(status().isUnauthorized());
  }

  // Liveness (e não /health): o health composto inclui Redis/SQS, que este contexto de teste não
  // sobe; o que se quer provar aqui é só que as sondas continuam públicas, sem token.
  @Test
  void asSondasDeHealthDevemContinuarPublicas() throws Exception {
    mockMvc.perform(get("/actuator/health/liveness")).andExpect(status().isOk());
  }
}

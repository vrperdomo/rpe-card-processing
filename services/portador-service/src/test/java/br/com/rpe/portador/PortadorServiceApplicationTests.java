package br.com.rpe.portador;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

// Relay do outbox desligado: este smoke test nao precisa do @Scheduled disparando em background
// enquanto o Postgres de Testcontainers pode ja estar sendo encerrado por outro teste.
@SpringBootTest(properties = "rpe.portador.outbox.relay.ativo=false")
class PortadorServiceApplicationTests extends IntegrationTestBase {

  @Test
  void contextLoads() {}
}

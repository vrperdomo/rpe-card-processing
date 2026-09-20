package br.com.rpe.cartao;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

// Auto-startup do listener SQS desligado: este smoke test não sobe LocalStack.
@SpringBootTest(properties = "rpe.cartao.mensageria.listener-auto-startup=false")
class CartaoServiceApplicationTests extends IntegrationTestBase {

  @Test
  void contextLoads() {}
}

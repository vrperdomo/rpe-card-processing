package br.com.rpe.cartao.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import org.junit.jupiter.api.Test;

class ValidadeTest {

  @Test
  void deveAceitarFormatoMmBarraYyValido() {
    Validade validade = Validade.of("12/29");

    assertThat(validade.valor()).isEqualTo("12/29");
  }

  @Test
  void deveRecusarMesInvalido() {
    assertThatThrownBy(() -> Validade.of("13/29")).isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> Validade.of("00/29")).isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void deveRecusarFormatoComSeparadorOuTamanhoErrado() {
    assertThatThrownBy(() -> Validade.of("2029-12")).isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> Validade.of("1/29")).isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void deveRecusarValorNulo() {
    assertThatThrownBy(() -> Validade.of(null)).isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void deveGerarValidadeCincoAnosAFrenteDaDataAtual() {
    Instant agora = Instant.parse("2026-09-17T12:00:00Z");

    Validade validade = Validade.gerar(agora);

    assertThat(validade.valor()).isEqualTo("09/31");
  }

  @Test
  void doisValoresIguaisDevemSerIguais() {
    assertThat(Validade.of("12/29")).isEqualTo(Validade.of("12/29"));
  }
}

package br.com.rpe.cartao.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class PanTest {

  private static final String PAN_VISA_TESTE_VALIDO = "4111111111111111";

  @Test
  void deveCriarPanAPartirDeValorValido() {
    Pan pan = Pan.of(PAN_VISA_TESTE_VALIDO);

    assertThat(pan.valor()).isEqualTo(PAN_VISA_TESTE_VALIDO);
  }

  @Test
  void deveRecusarPanComDigitoVerificadorInvalido() {
    assertThatThrownBy(() -> Pan.of("4111111111111112"))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Luhn");
  }

  @Test
  void deveRecusarPanComTamanhoDiferenteDe16Digitos() {
    assertThatThrownBy(() -> Pan.of("411111111111"))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("16 dígitos");
  }

  @Test
  void deveRecusarPanComCaracteresNaoNumericos() {
    assertThatThrownBy(() -> Pan.of("411111111111111a"))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void deveRecusarPanNulo() {
    assertThatThrownBy(() -> Pan.of(null)).isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void deveGerarPanValidoComBinInformado() {
    Pan pan = Pan.gerar("453201");

    assertThat(pan.valor()).hasSize(16).startsWith("453201");
  }

  @Test
  void deveGerarPanDiferenteACadaChamada() {
    Pan primeiro = Pan.gerar("453201");
    Pan segundo = Pan.gerar("453201");

    assertThat(primeiro).isNotEqualTo(segundo);
  }

  @Test
  void deveRecusarBinInvalidoAoGerar() {
    assertThatThrownBy(() -> Pan.gerar("12345"))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("bin");
  }

  @Test
  void deveMascararExibindoApenasUltimos4Digitos() {
    Pan pan = Pan.of(PAN_VISA_TESTE_VALIDO);

    assertThat(pan.ultimos4()).isEqualTo("1111");
    assertThat(pan.mascarado()).isEqualTo("**** **** **** 1111");
  }

  @Test
  void toStringNaoDeveExporPanCompleto() {
    Pan pan = Pan.of(PAN_VISA_TESTE_VALIDO);

    assertThat(pan.toString()).isEqualTo(pan.mascarado()).doesNotContain(PAN_VISA_TESTE_VALIDO);
  }

  @Test
  void doisPansComMesmoValorDevemSerIguais() {
    assertThat(Pan.of(PAN_VISA_TESTE_VALIDO)).isEqualTo(Pan.of(PAN_VISA_TESTE_VALIDO));
  }
}

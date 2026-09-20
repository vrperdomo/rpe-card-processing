package br.com.rpe.portador.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class CpfTest {

  @Test
  void deveAceitarCpfValido() {
    Cpf cpf = Cpf.of("529.982.247-25");

    assertThat(cpf.valor()).isEqualTo("52998224725");
  }

  @Test
  void deveAceitarCpfValidoSomenteComDigitos() {
    Cpf cpf = Cpf.of("52998224725");

    assertThat(cpf.valor()).isEqualTo("52998224725");
  }

  @Test
  void deveMascararParaExibicaoELog() {
    Cpf cpf = Cpf.of("52998224725");

    assertThat(cpf.mascarado()).isEqualTo("***.982.247-**");
    assertThat(cpf.toString()).isEqualTo("***.982.247-**");
  }

  @Test
  void deveConsiderarIguaisCpfsComMesmoValor() {
    assertThat(Cpf.of("529.982.247-25")).isEqualTo(Cpf.of("52998224725"));
  }

  @Test
  void deveRejeitarDigitoVerificadorInvalido() {
    assertThatThrownBy(() -> Cpf.of("52998224700")).isInstanceOf(IllegalArgumentException.class);
  }

  @ParameterizedTest
  @ValueSource(
      strings = {"00000000000", "11111111111", "22222222222", "33333333333", "99999999999"})
  void deveRejeitarSequenciaDeDigitosIguais(String sequencia) {
    assertThatThrownBy(() -> Cpf.of(sequencia)).isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void deveRejeitarQuantidadeDeDigitosDiferenteDeOnze() {
    assertThatThrownBy(() -> Cpf.of("1234567890")).isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void deveRejeitarValorNulo() {
    assertThatThrownBy(() -> Cpf.of(null)).isInstanceOf(IllegalArgumentException.class);
  }
}

package br.com.rpe.portador.adapters.in.web.validation;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class CpfValidoValidatorTest {

  private final CpfValidoValidator validator = new CpfValidoValidator();

  @Test
  void deveAceitarCpfValido() {
    assertThat(validator.isValid("529.982.247-25", null)).isTrue();
  }

  @Test
  void deveRejeitarCpfComDigitoVerificadorInvalido() {
    assertThat(validator.isValid("529.982.247-00", null)).isFalse();
  }

  @Test
  void deveAceitarValorNuloDelegandoParaNotBlank() {
    assertThat(validator.isValid(null, null)).isTrue();
  }
}

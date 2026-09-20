package br.com.rpe.cartao.adapters.out.security;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.rpe.cartao.config.PanCriptografiaProperties;
import org.junit.jupiter.api.Test;

class PanCriptografoAesGcmTest {

  private static final String PAN = "4532015112830366";

  private final PanCriptografoAesGcm criptografo =
      new PanCriptografoAesGcm(
          new PanCriptografiaProperties(
              "senha-de-teste-bem-forte", "a1b2c3d4e5f60718293a4b5c6d7e8f90", "pimenta-de-teste"));

  @Test
  void deveDecifrarParaOMesmoValorOriginal() {
    String cifrado = criptografo.cifrar(PAN);

    assertThat(criptografo.decifrar(cifrado)).isEqualTo(PAN);
  }

  @Test
  void naoDeveArmazenarOPanEmClaroNoValorCifrado() {
    String cifrado = criptografo.cifrar(PAN);

    assertThat(cifrado).doesNotContain(PAN);
  }

  @Test
  void devePermitirIvAleatorioPorChamadaProduzindoCifradosDiferentes() {
    String primeiro = criptografo.cifrar(PAN);
    String segundo = criptografo.cifrar(PAN);

    assertThat(primeiro).isNotEqualTo(segundo);
    assertThat(criptografo.decifrar(primeiro)).isEqualTo(PAN);
    assertThat(criptografo.decifrar(segundo)).isEqualTo(PAN);
  }

  @Test
  void hashDeveSerDeterministicoParaOMesmoPan() {
    assertThat(criptografo.hash(PAN)).isEqualTo(criptografo.hash(PAN));
  }

  @Test
  void hashDeveSerDiferenteParaPansDiferentes() {
    assertThat(criptografo.hash(PAN)).isNotEqualTo(criptografo.hash("5555555555554444"));
  }

  @Test
  void hashNuncaExpoeOPanEmClaro() {
    assertThat(criptografo.hash(PAN)).doesNotContain(PAN);
  }

  @Test
  void hashDeveTerSessentaEQuatroCaracteresHexadecimais() {
    assertThat(criptografo.hash(PAN)).hasSize(64).matches("[0-9a-f]{64}");
  }
}

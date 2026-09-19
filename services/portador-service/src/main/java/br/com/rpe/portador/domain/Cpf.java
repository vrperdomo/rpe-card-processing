package br.com.rpe.portador.domain;

import java.util.Objects;
import java.util.regex.Pattern;

public final class Cpf {

  private static final Pattern SOMENTE_DIGITOS = Pattern.compile("\\d+");
  private static final int TAMANHO = 11;

  private final String valor;

  private Cpf(String valor) {
    this.valor = validar(valor);
  }

  public static Cpf of(String valor) {
    return new Cpf(valor);
  }

  private static String validar(String valorBruto) {
    if (valorBruto == null) {
      throw new IllegalArgumentException("CPF não pode ser nulo");
    }
    String digitos = valorBruto.replaceAll("\\D", "");
    if (digitos.length() != TAMANHO || !SOMENTE_DIGITOS.matcher(digitos).matches()) {
      throw new IllegalArgumentException(
          "CPF deve conter exatamente %d dígitos".formatted(TAMANHO));
    }
    if (todosOsDigitosIguais(digitos)) {
      throw new IllegalArgumentException("CPF inválido");
    }
    if (!digitosVerificadoresValidos(digitos)) {
      throw new IllegalArgumentException("CPF inválido: dígito verificador não confere");
    }
    return digitos;
  }

  private static boolean todosOsDigitosIguais(String digitos) {
    return digitos.chars().distinct().count() == 1;
  }

  private static boolean digitosVerificadoresValidos(String digitos) {
    int d10 = calcularDigitoVerificador(digitos.substring(0, 9), 10);
    int d11 = calcularDigitoVerificador(digitos.substring(0, 9) + d10, 11);
    return digitos.charAt(9) - '0' == d10 && digitos.charAt(10) - '0' == d11;
  }

  private static int calcularDigitoVerificador(String base, int pesoInicial) {
    int soma = 0;
    int peso = pesoInicial;
    for (int i = 0; i < base.length(); i++) {
      soma += (base.charAt(i) - '0') * peso;
      peso--;
    }
    int resto = (soma * 10) % 11;
    return resto == 10 ? 0 : resto;
  }

  public String valor() {
    return valor;
  }

  public String mascarado() {
    return "***." + valor.substring(3, 6) + "." + valor.substring(6, 9) + "-**";
  }

  @Override
  public String toString() {
    return mascarado();
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof Cpf other)) {
      return false;
    }
    return valor.equals(other.valor);
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(valor);
  }
}

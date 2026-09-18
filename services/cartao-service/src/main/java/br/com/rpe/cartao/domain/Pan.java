package br.com.rpe.cartao.domain;

import java.security.SecureRandom;
import java.util.Objects;
import java.util.regex.Pattern;

public final class Pan {

  private static final Pattern SOMENTE_DIGITOS = Pattern.compile("\\d+");
  private static final Pattern BIN_VALIDO = Pattern.compile("\\d{6}");
  private static final int TAMANHO = 16;
  private static final SecureRandom GERADOR_ALEATORIO = new SecureRandom();

  private final String valor;

  private Pan(String valor) {
    this.valor = validar(valor);
  }

  public static Pan of(String valor) {
    return new Pan(valor);
  }

  public static Pan gerar(String bin) {
    if (bin == null || !BIN_VALIDO.matcher(bin).matches()) {
      throw new IllegalArgumentException("bin deve conter exatamente 6 dígitos numéricos");
    }
    StringBuilder parcial = new StringBuilder(bin);
    int digitosAleatoriosRestantes = TAMANHO - bin.length() - 1;
    for (int i = 0; i < digitosAleatoriosRestantes; i++) {
      parcial.append(GERADOR_ALEATORIO.nextInt(10));
    }
    parcial.append(digitoVerificador(parcial.toString()));
    return new Pan(parcial.toString());
  }

  private static String validar(String valor) {
    if (valor == null || valor.length() != TAMANHO || !SOMENTE_DIGITOS.matcher(valor).matches()) {
      throw new IllegalArgumentException(
          "PAN deve conter exatamente %d dígitos numéricos".formatted(TAMANHO));
    }
    if (!luhnValido(valor)) {
      throw new IllegalArgumentException("PAN inválido: falha na verificação de Luhn");
    }
    return valor;
  }

  private static boolean luhnValido(String numero) {
    int soma = 0;
    boolean dobrar = false;
    for (int i = numero.length() - 1; i >= 0; i--) {
      int digito = numero.charAt(i) - '0';
      if (dobrar) {
        digito *= 2;
        if (digito > 9) {
          digito -= 9;
        }
      }
      soma += digito;
      dobrar = !dobrar;
    }
    return soma % 10 == 0;
  }

  private static int digitoVerificador(String numeroParcial) {
    int soma = 0;
    boolean dobrar = true;
    for (int i = numeroParcial.length() - 1; i >= 0; i--) {
      int digito = numeroParcial.charAt(i) - '0';
      if (dobrar) {
        digito *= 2;
        if (digito > 9) {
          digito -= 9;
        }
      }
      soma += digito;
      dobrar = !dobrar;
    }
    int resto = soma % 10;
    return resto == 0 ? 0 : 10 - resto;
  }

  public String valor() {
    return valor;
  }

  public String ultimos4() {
    return valor.substring(valor.length() - 4);
  }

  public String mascarado() {
    return "**** **** **** " + ultimos4();
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
    if (!(o instanceof Pan other)) {
      return false;
    }
    return valor.equals(other.valor);
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(valor);
  }
}

package br.com.rpe.cartao.domain;

import java.time.Instant;
import java.time.YearMonth;
import java.time.ZoneOffset;
import java.util.Objects;
import java.util.regex.Pattern;

public final class Validade {

  private static final Pattern FORMATO_MM_YY = Pattern.compile("(0[1-9]|1[0-2])/\\d{2}");
  private static final int ANOS_DE_VALIDADE = 5;

  private final String valor;

  private Validade(String valor) {
    this.valor = validar(valor);
  }

  public static Validade of(String valor) {
    return new Validade(valor);
  }

  public static Validade gerar(Instant agora) {
    Objects.requireNonNull(agora, "agora não pode ser nulo");
    YearMonth expiracao = YearMonth.from(agora.atZone(ZoneOffset.UTC)).plusYears(ANOS_DE_VALIDADE);
    return new Validade(
        "%02d/%02d".formatted(expiracao.getMonthValue(), expiracao.getYear() % 100));
  }

  private static String validar(String valor) {
    if (valor == null || !FORMATO_MM_YY.matcher(valor).matches()) {
      throw new IllegalArgumentException("validade deve estar no formato MM/YY");
    }
    return valor;
  }

  public String valor() {
    return valor;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof Validade other)) {
      return false;
    }
    return valor.equals(other.valor);
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(valor);
  }

  @Override
  public String toString() {
    return valor;
  }
}

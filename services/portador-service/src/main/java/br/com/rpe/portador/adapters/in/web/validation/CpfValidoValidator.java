package br.com.rpe.portador.adapters.in.web.validation;

import br.com.rpe.portador.domain.Cpf;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

// Delega para o VO de dominio (mesma regra, sem duplicar o algoritmo de digito verificador).
public class CpfValidoValidator implements ConstraintValidator<CpfValido, String> {

  @Override
  public boolean isValid(String valor, ConstraintValidatorContext context) {
    if (valor == null) {
      return true; // @NotBlank cuida da ausencia; validador de formato nao se aplica a nulo.
    }
    try {
      Cpf.of(valor);
      return true;
    } catch (IllegalArgumentException ex) {
      return false;
    }
  }
}

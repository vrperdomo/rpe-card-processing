package br.com.rpe.portador.application.port.out;

public interface VerificadorSenha {

  boolean confere(String senhaBruta, String hashArmazenado);
}

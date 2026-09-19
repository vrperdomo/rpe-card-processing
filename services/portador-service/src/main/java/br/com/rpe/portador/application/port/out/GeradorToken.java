package br.com.rpe.portador.application.port.out;

public interface GeradorToken {

  Token gerar(String subject);

  record Token(String valor, long expiraEmSegundos) {}
}

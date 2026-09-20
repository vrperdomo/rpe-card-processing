package br.com.rpe.cartao.application.port.out;

public interface CriptografoPan {

  String cifrar(String panEmClaro);

  String decifrar(String panCifrado);

  String hash(String panEmClaro);
}

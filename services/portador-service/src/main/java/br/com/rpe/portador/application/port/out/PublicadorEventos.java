package br.com.rpe.portador.application.port.out;

import br.com.rpe.portador.application.evento.EventoPendente;

public interface PublicadorEventos {

  void publicar(EventoPendente evento);
}

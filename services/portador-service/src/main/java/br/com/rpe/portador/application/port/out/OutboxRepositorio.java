package br.com.rpe.portador.application.port.out;

import br.com.rpe.portador.application.evento.EventoOutbox;
import java.util.UUID;

public interface OutboxRepositorio {

  void registrar(UUID aggregateId, String aggregateType, EventoOutbox evento);
}

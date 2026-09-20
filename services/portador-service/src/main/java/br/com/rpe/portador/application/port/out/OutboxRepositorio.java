package br.com.rpe.portador.application.port.out;

import br.com.rpe.portador.application.evento.EventoOutbox;
import br.com.rpe.portador.application.evento.EventoPendente;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface OutboxRepositorio {

  void registrar(UUID aggregateId, String aggregateType, EventoOutbox evento);

  List<EventoPendente> buscarLotePendente(int tamanhoLote);

  void marcarPublicado(UUID eventId, Instant publicadoEm);

  void marcarFalhaTemporaria(UUID eventId, Instant proximaTentativaEm, String erro);

  void marcarFalhaDefinitiva(UUID eventId, String erro);
}

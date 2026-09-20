package br.com.rpe.cartao.application.port.out;

import java.time.Instant;
import java.util.UUID;

public interface MensagemProcessadaRepositorio {

  boolean jaProcessada(UUID eventId);

  void marcarProcessada(UUID eventId, Instant processadoEm);
}

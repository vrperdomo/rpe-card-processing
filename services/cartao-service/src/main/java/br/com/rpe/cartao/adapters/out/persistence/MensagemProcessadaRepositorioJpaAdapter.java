package br.com.rpe.cartao.adapters.out.persistence;

import br.com.rpe.cartao.application.port.out.MensagemProcessadaRepositorio;
import java.time.Instant;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class MensagemProcessadaRepositorioJpaAdapter implements MensagemProcessadaRepositorio {

  private final MensagemProcessadaJpaRepository jpaRepository;

  public MensagemProcessadaRepositorioJpaAdapter(MensagemProcessadaJpaRepository jpaRepository) {
    this.jpaRepository = jpaRepository;
  }

  @Override
  public boolean jaProcessada(UUID eventId) {
    return jpaRepository.existsById(eventId);
  }

  @Override
  public void marcarProcessada(UUID eventId, Instant processadoEm) {
    jpaRepository.save(new MensagemProcessadaEntity(eventId, processadoEm));
  }
}

package br.com.rpe.cartao.adapters.out.persistence;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MensagemProcessadaJpaRepository
    extends JpaRepository<MensagemProcessadaEntity, UUID> {}

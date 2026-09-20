package br.com.rpe.portador.adapters.out.persistence;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PortadorJpaRepository extends JpaRepository<PortadorEntity, UUID> {

  boolean existsByCpf(String cpf);
}

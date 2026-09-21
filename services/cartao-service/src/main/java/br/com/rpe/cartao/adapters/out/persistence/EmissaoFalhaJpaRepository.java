package br.com.rpe.cartao.adapters.out.persistence;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface EmissaoFalhaJpaRepository extends JpaRepository<EmissaoFalhaEntity, UUID> {

  // Um único DELETE: roda a cada emissão bem-sucedida, então não pode custar um SELECT antes.
  @Modifying
  @Query("delete from EmissaoFalhaEntity f where f.portadorId = :portadorId")
  void deletarPorPortadorId(@Param("portadorId") UUID portadorId);
}

package br.com.rpe.portador.adapters.out.persistence;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface OutboxEventJpaRepository extends JpaRepository<OutboxEventEntity, UUID> {

  // FOR UPDATE SKIP LOCKED: seguro com multiplas replicas do relay rodando ao mesmo tempo, cada
  // uma pula as linhas ja travadas por outra em vez de bloquear esperando (PRD 9.1).
  @Query(
      value =
          """
          SELECT * FROM outbox_event
          WHERE status = 'PENDENTE' AND (proxima_tentativa_em IS NULL OR proxima_tentativa_em <= :agora)
          ORDER BY criado_em ASC
          LIMIT :lote
          FOR UPDATE SKIP LOCKED
          """,
      nativeQuery = true)
  List<OutboxEventEntity> buscarLotePendente(
      @Param("agora") Instant agora, @Param("lote") int lote);

  long countByStatus(StatusOutboxEvent status);
}

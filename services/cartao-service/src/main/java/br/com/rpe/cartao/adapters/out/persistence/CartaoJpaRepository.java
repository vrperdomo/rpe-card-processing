package br.com.rpe.cartao.adapters.out.persistence;

import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CartaoJpaRepository extends JpaRepository<CartaoEntity, UUID> {

  boolean existsByPortadorIdAndProdutoId(UUID portadorId, UUID produtoId);

  boolean existsByPanHash(String panHash);

  Page<CartaoEntity> findByPortadorId(UUID portadorId, Pageable pageable);
}

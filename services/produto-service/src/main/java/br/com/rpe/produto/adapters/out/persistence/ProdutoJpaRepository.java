package br.com.rpe.produto.adapters.out.persistence;

import br.com.rpe.produto.domain.StatusProduto;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProdutoJpaRepository extends JpaRepository<ProdutoEntity, UUID> {

  boolean existsByNome(String nome);

  Page<ProdutoEntity> findByStatus(StatusProduto status, Pageable pageable);
}

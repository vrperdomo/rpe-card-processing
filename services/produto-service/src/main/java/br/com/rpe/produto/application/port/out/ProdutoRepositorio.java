package br.com.rpe.produto.application.port.out;

import br.com.rpe.produto.domain.Produto;
import br.com.rpe.produto.domain.StatusProduto;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface ProdutoRepositorio {

  Produto salvar(Produto produto);

  Optional<Produto> buscarPorId(UUID id);

  boolean existePorNome(String nome);

  Page<Produto> listar(StatusProduto status, Pageable pageable);
}

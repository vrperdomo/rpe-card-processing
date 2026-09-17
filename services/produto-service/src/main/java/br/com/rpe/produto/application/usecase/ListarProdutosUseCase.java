package br.com.rpe.produto.application.usecase;

import br.com.rpe.produto.application.port.out.ProdutoRepositorio;
import br.com.rpe.produto.domain.Produto;
import br.com.rpe.produto.domain.StatusProduto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ListarProdutosUseCase {

  private final ProdutoRepositorio produtoRepositorio;

  public ListarProdutosUseCase(ProdutoRepositorio produtoRepositorio) {
    this.produtoRepositorio = produtoRepositorio;
  }

  @Transactional(readOnly = true)
  public Page<Produto> executar(StatusProduto status, Pageable pageable) {
    return produtoRepositorio.listar(status, pageable);
  }
}

package br.com.rpe.produto.application.usecase;

import br.com.rpe.produto.application.port.out.ProdutoRepositorio;
import br.com.rpe.produto.domain.Produto;
import br.com.rpe.produto.domain.exception.RecursoNaoEncontradoException;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class BuscarProdutoUseCase {

  private final ProdutoRepositorio produtoRepositorio;

  public BuscarProdutoUseCase(ProdutoRepositorio produtoRepositorio) {
    this.produtoRepositorio = produtoRepositorio;
  }

  @Transactional(readOnly = true)
  public Produto executar(UUID id) {
    return produtoRepositorio
        .buscarPorId(id)
        .orElseThrow(
            () -> new RecursoNaoEncontradoException("Produto %s não encontrado".formatted(id)));
  }
}

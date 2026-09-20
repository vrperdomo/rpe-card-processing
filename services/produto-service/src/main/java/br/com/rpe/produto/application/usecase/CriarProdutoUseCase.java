package br.com.rpe.produto.application.usecase;

import br.com.rpe.produto.application.port.out.ProdutoRepositorio;
import br.com.rpe.produto.domain.CategoriaProduto;
import br.com.rpe.produto.domain.Produto;
import br.com.rpe.produto.domain.exception.ConflitoException;
import java.time.Clock;
import java.time.Instant;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CriarProdutoUseCase {

  private final ProdutoRepositorio produtoRepositorio;
  private final Clock clock;

  public CriarProdutoUseCase(ProdutoRepositorio produtoRepositorio, Clock clock) {
    this.produtoRepositorio = produtoRepositorio;
    this.clock = clock;
  }

  @Transactional
  public Produto executar(String nome, String descricao, CategoriaProduto categoria, String bin) {
    if (produtoRepositorio.existePorNome(nome)) {
      throw new ConflitoException("Já existe um produto com o nome '%s'".formatted(nome));
    }
    Produto produto = Produto.criar(nome, descricao, categoria, bin, Instant.now(clock));
    return produtoRepositorio.salvar(produto);
  }
}

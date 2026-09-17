package br.com.rpe.produto.application.usecase;

import br.com.rpe.produto.application.port.out.ProdutoRepositorio;
import br.com.rpe.produto.domain.Produto;
import br.com.rpe.produto.domain.StatusProduto;
import br.com.rpe.produto.domain.exception.RecursoNaoEncontradoException;
import br.com.rpe.produto.domain.exception.RegraNegocioException;
import java.time.Clock;
import java.time.Instant;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AlterarStatusProdutoUseCase {

  private final ProdutoRepositorio produtoRepositorio;
  private final Clock clock;

  public AlterarStatusProdutoUseCase(ProdutoRepositorio produtoRepositorio, Clock clock) {
    this.produtoRepositorio = produtoRepositorio;
    this.clock = clock;
  }

  @Transactional
  public Produto executar(UUID id, StatusProduto novoStatus) {
    if (novoStatus != StatusProduto.CANCELADO) {
      throw new RegraNegocioException(
          "Transição de status para %s não é permitida".formatted(novoStatus));
    }
    Produto produto =
        produtoRepositorio
            .buscarPorId(id)
            .orElseThrow(
                () -> new RecursoNaoEncontradoException("Produto %s não encontrado".formatted(id)));
    produto.cancelar(Instant.now(clock));
    return produtoRepositorio.salvar(produto);
  }
}

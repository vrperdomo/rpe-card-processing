package br.com.rpe.cartao.application.usecase;

import br.com.rpe.cartao.application.port.out.CartaoRepositorio;
import br.com.rpe.cartao.application.port.out.ProdutoClient;
import br.com.rpe.cartao.application.port.out.ProdutoDto;
import br.com.rpe.cartao.domain.Cartao;
import br.com.rpe.cartao.domain.exception.RecursoNaoEncontradoException;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class BuscarCartaoUseCase {

  private final CartaoRepositorio cartaoRepositorio;
  private final ProdutoClient produtoClient;

  public BuscarCartaoUseCase(CartaoRepositorio cartaoRepositorio, ProdutoClient produtoClient) {
    this.cartaoRepositorio = cartaoRepositorio;
    this.produtoClient = produtoClient;
  }

  @Transactional(readOnly = true)
  public CartaoComProduto executar(UUID id) {
    Cartao cartao =
        cartaoRepositorio
            .buscarPorId(id)
            .orElseThrow(
                () -> new RecursoNaoEncontradoException("Cartão %s não encontrado".formatted(id)));
    Optional<ProdutoDto> produto = produtoClient.buscarPorId(cartao.getProdutoId());
    return new CartaoComProduto(cartao, produto);
  }
}

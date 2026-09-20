package br.com.rpe.cartao.application.usecase;

import br.com.rpe.cartao.application.port.out.CartaoRepositorio;
import br.com.rpe.cartao.application.port.out.ProdutoClient;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ListarCartoesPorPortadorUseCase {

  private final CartaoRepositorio cartaoRepositorio;
  private final ProdutoClient produtoClient;

  public ListarCartoesPorPortadorUseCase(
      CartaoRepositorio cartaoRepositorio, ProdutoClient produtoClient) {
    this.cartaoRepositorio = cartaoRepositorio;
    this.produtoClient = produtoClient;
  }

  @Transactional(readOnly = true)
  public Page<CartaoComProduto> executar(UUID portadorId, Pageable pageable) {
    return cartaoRepositorio
        .buscarPorPortadorId(portadorId, pageable)
        .map(
            cartao ->
                new CartaoComProduto(cartao, produtoClient.buscarPorId(cartao.getProdutoId())));
  }
}

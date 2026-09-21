package br.com.rpe.cartao.application.usecase;

import br.com.rpe.cartao.application.port.out.ProdutoClient;
import br.com.rpe.cartao.application.port.out.ProdutoDto;
import br.com.rpe.cartao.application.seguranca.Solicitante;
import br.com.rpe.cartao.domain.Cartao;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class BuscarCartaoUseCase {

  private final AcessoAoCartao acessoAoCartao;
  private final ProdutoClient produtoClient;

  public BuscarCartaoUseCase(AcessoAoCartao acessoAoCartao, ProdutoClient produtoClient) {
    this.acessoAoCartao = acessoAoCartao;
    this.produtoClient = produtoClient;
  }

  @Transactional(readOnly = true)
  public CartaoComProduto executar(UUID id, Solicitante solicitante) {
    Cartao cartao = acessoAoCartao.obter(id, solicitante);
    Optional<ProdutoDto> produto = produtoClient.buscarPorId(cartao.getProdutoId());
    return new CartaoComProduto(cartao, produto);
  }
}

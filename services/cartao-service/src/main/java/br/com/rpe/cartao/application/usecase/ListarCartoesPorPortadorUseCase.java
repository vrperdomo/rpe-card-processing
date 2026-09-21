package br.com.rpe.cartao.application.usecase;

import br.com.rpe.cartao.application.port.out.CartaoRepositorio;
import br.com.rpe.cartao.application.port.out.ProdutoClient;
import br.com.rpe.cartao.application.seguranca.Solicitante;
import br.com.rpe.cartao.domain.Cartao;
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
  public Page<CartaoComProduto> executar(
      UUID portadorId, Pageable pageable, Solicitante solicitante) {
    return paginaVisivel(portadorId, pageable, solicitante)
        .map(
            cartao ->
                new CartaoComProduto(cartao, produtoClient.buscarPorId(cartao.getProdutoId())));
  }

  // ADR-009 (A01): o serviço (Portador no /completo) vê todos; o usuário, só os que criou. Filtrar
  // no banco mantém a paginação correta (total e páginas refletem apenas o que ele pode ver).
  private Page<Cartao> paginaVisivel(UUID portadorId, Pageable pageable, Solicitante solicitante) {
    if (solicitante.servico()) {
      return cartaoRepositorio.buscarPorPortadorId(portadorId, pageable);
    }
    // Mesma regra do acesso por id: um usuário chamado como o dono sentinela não herda o legado.
    if (!solicitante.podeAcessar(solicitante.id())) {
      return Page.empty(pageable);
    }
    return cartaoRepositorio.buscarPorPortadorIdEDono(portadorId, solicitante.id(), pageable);
  }
}

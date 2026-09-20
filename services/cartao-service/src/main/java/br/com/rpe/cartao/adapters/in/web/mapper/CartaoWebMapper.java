package br.com.rpe.cartao.adapters.in.web.mapper;

import br.com.rpe.cartao.adapters.in.web.dto.CartaoResponse;
import br.com.rpe.cartao.adapters.in.web.dto.ProdutoResumoResponse;
import br.com.rpe.cartao.application.port.out.ProdutoDto;
import br.com.rpe.cartao.domain.Cartao;
import java.util.Optional;
import org.springframework.stereotype.Component;

// Nao usa MapStruct de proposito: a resposta combina duas fontes (Cartao do proprio banco e
// ProdutoDto vindo do ProdutoClient), o que foge do estilo de mapeamento 1:1 declarativo.
@Component
public class CartaoWebMapper {

  // Usado pelo PATCH de status: alterar status nao precisa dos dados do produto, e poupa a
  // chamada ao Produto Service num endpoint de escrita.
  public CartaoResponse paraResponse(Cartao cartao) {
    return paraResponse(cartao, Optional.empty());
  }

  public CartaoResponse paraResponse(Cartao cartao, Optional<ProdutoDto> produto) {
    return new CartaoResponse(
        cartao.getId(),
        cartao.getPortadorId(),
        cartao.getProdutoId(),
        cartao.getPan().mascarado(),
        cartao.getNomeImpresso(),
        cartao.getValidade().valor(),
        cartao.getStatus(),
        produto.map(this::paraResumo).orElse(null),
        cartao.getCriadoEm(),
        cartao.getAtualizadoEm());
  }

  private ProdutoResumoResponse paraResumo(ProdutoDto produto) {
    return new ProdutoResumoResponse(
        produto.id(), produto.nome(), produto.categoria(), produto.status());
  }
}

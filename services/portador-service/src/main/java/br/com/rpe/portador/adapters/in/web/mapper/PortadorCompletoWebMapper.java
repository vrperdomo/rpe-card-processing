package br.com.rpe.portador.adapters.in.web.mapper;

import br.com.rpe.portador.adapters.in.web.dto.CartaoResumoResponse;
import br.com.rpe.portador.adapters.in.web.dto.PortadorCompletoResponse;
import br.com.rpe.portador.adapters.in.web.dto.ProdutoResumoResponse;
import br.com.rpe.portador.application.port.out.CartaoDto;
import br.com.rpe.portador.application.port.out.ProdutoDto;
import br.com.rpe.portador.application.usecase.PortadorCompleto;
import org.springframework.stereotype.Component;

// Nao usa MapStruct de proposito: combina quatro fontes (Portador, Optional<CartaoDto>,
// Optional<ProdutoDto>, avisos), o que foge do estilo de mapeamento 1:1 declarativo — mesmo
// racional do CartaoWebMapper no Cartao Service.
@Component
public class PortadorCompletoWebMapper {

  private final PortadorWebMapper portadorWebMapper;

  public PortadorCompletoWebMapper(PortadorWebMapper portadorWebMapper) {
    this.portadorWebMapper = portadorWebMapper;
  }

  public PortadorCompletoResponse paraResponse(PortadorCompleto completo) {
    return new PortadorCompletoResponse(
        portadorWebMapper.paraResponse(completo.portador()),
        completo.cartao().map(this::paraResumoCartao).orElse(null),
        completo.produto().map(this::paraResumoProduto).orElse(null),
        completo.emissao(),
        completo.avisos());
  }

  private CartaoResumoResponse paraResumoCartao(CartaoDto cartao) {
    return new CartaoResumoResponse(
        cartao.id(), cartao.panMascarado(), cartao.validade(), cartao.status());
  }

  private ProdutoResumoResponse paraResumoProduto(ProdutoDto produto) {
    return new ProdutoResumoResponse(
        produto.id(), produto.nome(), produto.categoria(), produto.status());
  }
}

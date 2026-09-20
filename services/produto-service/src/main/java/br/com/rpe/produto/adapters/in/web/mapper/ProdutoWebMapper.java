package br.com.rpe.produto.adapters.in.web.mapper;

import br.com.rpe.produto.adapters.in.web.dto.ProdutoResponse;
import br.com.rpe.produto.domain.Produto;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface ProdutoWebMapper {

  ProdutoResponse paraResponse(Produto produto);
}

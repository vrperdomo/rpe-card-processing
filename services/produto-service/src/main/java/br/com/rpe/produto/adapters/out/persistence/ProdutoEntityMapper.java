package br.com.rpe.produto.adapters.out.persistence;

import br.com.rpe.produto.domain.Produto;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface ProdutoEntityMapper {

  default ProdutoEntity paraNovaEntidade(Produto produto) {
    return new ProdutoEntity(
        produto.getId(),
        produto.getNome(),
        produto.getDescricao(),
        produto.getCategoria(),
        produto.getBin(),
        produto.getStatus());
  }

  @Mapping(target = "id", ignore = true)
  @Mapping(target = "versao", ignore = true)
  @Mapping(target = "criadoEm", ignore = true)
  @Mapping(target = "atualizadoEm", ignore = true)
  void copiarParaEntidadeExistente(Produto origem, @MappingTarget ProdutoEntity destino);

  default Produto paraDominio(ProdutoEntity entity) {
    return Produto.reconstituir(
        entity.getId(),
        entity.getNome(),
        entity.getDescricao(),
        entity.getCategoria(),
        entity.getBin(),
        entity.getStatus(),
        entity.getCriadoEm(),
        entity.getAtualizadoEm());
  }
}

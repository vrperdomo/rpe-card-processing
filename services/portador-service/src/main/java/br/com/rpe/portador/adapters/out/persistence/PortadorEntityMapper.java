package br.com.rpe.portador.adapters.out.persistence;

import br.com.rpe.portador.domain.Cpf;
import br.com.rpe.portador.domain.Portador;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface PortadorEntityMapper {

  default PortadorEntity paraNovaEntidade(Portador portador) {
    return new PortadorEntity(
        portador.getId(),
        portador.getNome(),
        portador.getCpf().valor(),
        portador.getDataNascimento(),
        portador.getProdutoId(),
        portador.getStatus());
  }

  default void copiarParaEntidadeExistente(Portador origem, PortadorEntity destino) {
    destino.setNome(origem.getNome());
    destino.setStatus(origem.getStatus());
  }

  default Portador paraDominio(PortadorEntity entity) {
    return Portador.reconstituir(
        entity.getId(),
        entity.getNome(),
        Cpf.of(entity.getCpf()),
        entity.getDataNascimento(),
        entity.getProdutoId(),
        entity.getStatus(),
        entity.getCriadoEm(),
        entity.getAtualizadoEm());
  }
}

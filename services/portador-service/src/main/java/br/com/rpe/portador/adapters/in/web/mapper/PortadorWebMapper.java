package br.com.rpe.portador.adapters.in.web.mapper;

import br.com.rpe.portador.adapters.in.web.dto.PortadorResponse;
import br.com.rpe.portador.domain.Portador;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface PortadorWebMapper {

  @Mapping(target = "cpf", expression = "java(portador.getCpf().mascarado())")
  PortadorResponse paraResponse(Portador portador);
}

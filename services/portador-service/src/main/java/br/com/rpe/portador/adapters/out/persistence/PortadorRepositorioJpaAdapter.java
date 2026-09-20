package br.com.rpe.portador.adapters.out.persistence;

import br.com.rpe.portador.application.port.out.PortadorRepositorio;
import br.com.rpe.portador.domain.Portador;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class PortadorRepositorioJpaAdapter implements PortadorRepositorio {

  private final PortadorJpaRepository jpaRepository;
  private final PortadorEntityMapper mapper;

  public PortadorRepositorioJpaAdapter(
      PortadorJpaRepository jpaRepository, PortadorEntityMapper mapper) {
    this.jpaRepository = jpaRepository;
    this.mapper = mapper;
  }

  @Override
  public Portador salvar(Portador portador) {
    PortadorEntity entity =
        jpaRepository
            .findById(portador.getId())
            .map(
                existente -> {
                  mapper.copiarParaEntidadeExistente(portador, existente);
                  return existente;
                })
            .orElseGet(() -> mapper.paraNovaEntidade(portador));
    return mapper.paraDominio(jpaRepository.save(entity));
  }

  @Override
  public Optional<Portador> buscarPorId(UUID id) {
    return jpaRepository.findById(id).map(mapper::paraDominio);
  }

  @Override
  public boolean existePorCpf(String cpf) {
    return jpaRepository.existsByCpf(cpf);
  }
}

package br.com.rpe.cartao.adapters.out.persistence;

import br.com.rpe.cartao.application.port.out.EmissaoFalhaRepositorio;
import br.com.rpe.cartao.domain.EmissaoFalha;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class EmissaoFalhaRepositorioJpaAdapter implements EmissaoFalhaRepositorio {

  private final EmissaoFalhaJpaRepository jpaRepository;

  public EmissaoFalhaRepositorioJpaAdapter(EmissaoFalhaJpaRepository jpaRepository) {
    this.jpaRepository = jpaRepository;
  }

  @Override
  public void registrar(EmissaoFalha falha) {
    jpaRepository.save(
        new EmissaoFalhaEntity(
            falha.portadorId(),
            falha.produtoId(),
            falha.motivo(),
            falha.criadoPor(),
            falha.ocorridaEm()));
  }

  @Override
  public Optional<EmissaoFalha> buscarPorPortadorId(UUID portadorId) {
    return jpaRepository.findById(portadorId).map(this::paraDominio);
  }

  @Override
  public void removerPorPortadorId(UUID portadorId) {
    jpaRepository.deletarPorPortadorId(portadorId);
  }

  private EmissaoFalha paraDominio(EmissaoFalhaEntity entity) {
    return new EmissaoFalha(
        entity.getPortadorId(),
        entity.getProdutoId(),
        entity.getMotivo(),
        entity.getCriadoPor(),
        entity.getOcorridaEm());
  }
}

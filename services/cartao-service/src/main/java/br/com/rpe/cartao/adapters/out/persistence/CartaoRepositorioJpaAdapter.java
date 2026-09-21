package br.com.rpe.cartao.adapters.out.persistence;

import br.com.rpe.cartao.application.port.out.CartaoRepositorio;
import br.com.rpe.cartao.domain.Cartao;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

@Component
public class CartaoRepositorioJpaAdapter implements CartaoRepositorio {

  private final CartaoJpaRepository jpaRepository;
  private final CartaoEntityMapper mapper;

  public CartaoRepositorioJpaAdapter(CartaoJpaRepository jpaRepository, CartaoEntityMapper mapper) {
    this.jpaRepository = jpaRepository;
    this.mapper = mapper;
  }

  @Override
  public Cartao salvar(Cartao cartao) {
    CartaoEntity entity =
        jpaRepository
            .findById(cartao.getId())
            .map(
                existente -> {
                  mapper.copiarParaEntidadeExistente(cartao, existente);
                  return existente;
                })
            .orElseGet(() -> mapper.paraNovaEntidade(cartao));
    return mapper.paraDominio(jpaRepository.save(entity));
  }

  @Override
  public Optional<Cartao> buscarPorId(UUID id) {
    return jpaRepository.findById(id).map(mapper::paraDominio);
  }

  @Override
  public boolean existePorPortadorEProduto(UUID portadorId, UUID produtoId) {
    return jpaRepository.existsByPortadorIdAndProdutoId(portadorId, produtoId);
  }

  @Override
  public Page<Cartao> buscarPorPortadorId(UUID portadorId, Pageable pageable) {
    return jpaRepository.findByPortadorId(portadorId, pageable).map(mapper::paraDominio);
  }

  @Override
  public Page<Cartao> buscarPorPortadorIdEDono(
      UUID portadorId, String criadoPor, Pageable pageable) {
    return jpaRepository
        .findByPortadorIdAndCriadoPor(portadorId, criadoPor, pageable)
        .map(mapper::paraDominio);
  }
}

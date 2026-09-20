package br.com.rpe.cartao.adapters.out.persistence;

import br.com.rpe.cartao.application.port.out.CartaoRepositorio;
import br.com.rpe.cartao.domain.Cartao;
import java.util.Optional;
import java.util.UUID;
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
}

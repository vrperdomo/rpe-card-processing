package br.com.rpe.produto.adapters.out.persistence;

import br.com.rpe.produto.application.port.out.ProdutoRepositorio;
import br.com.rpe.produto.domain.Produto;
import br.com.rpe.produto.domain.StatusProduto;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

@Component
public class ProdutoRepositorioJpaAdapter implements ProdutoRepositorio {

  private final ProdutoJpaRepository jpaRepository;
  private final ProdutoEntityMapper mapper;

  public ProdutoRepositorioJpaAdapter(
      ProdutoJpaRepository jpaRepository, ProdutoEntityMapper mapper) {
    this.jpaRepository = jpaRepository;
    this.mapper = mapper;
  }

  @Override
  public Produto salvar(Produto produto) {
    ProdutoEntity entity =
        jpaRepository
            .findById(produto.getId())
            .map(
                existente -> {
                  mapper.copiarParaEntidadeExistente(produto, existente);
                  return existente;
                })
            .orElseGet(() -> mapper.paraNovaEntidade(produto));
    return mapper.paraDominio(jpaRepository.save(entity));
  }

  @Override
  public Optional<Produto> buscarPorId(UUID id) {
    return jpaRepository.findById(id).map(mapper::paraDominio);
  }

  @Override
  public boolean existePorNome(String nome) {
    return jpaRepository.existsByNome(nome);
  }

  @Override
  public Page<Produto> listar(StatusProduto status, Pageable pageable) {
    Page<ProdutoEntity> pagina =
        status == null
            ? jpaRepository.findAll(pageable)
            : jpaRepository.findByStatus(status, pageable);
    return pagina.map(mapper::paraDominio);
  }
}

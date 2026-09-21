package br.com.rpe.cartao.application.port.out;

import br.com.rpe.cartao.domain.Cartao;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface CartaoRepositorio {

  Cartao salvar(Cartao cartao);

  Optional<Cartao> buscarPorId(UUID id);

  boolean existePorPortadorEProduto(UUID portadorId, UUID produtoId);

  Page<Cartao> buscarPorPortadorId(UUID portadorId, Pageable pageable);

  Page<Cartao> buscarPorPortadorIdEDono(UUID portadorId, String criadoPor, Pageable pageable);
}

package br.com.rpe.cartao.application.port.out;

import br.com.rpe.cartao.domain.Cartao;
import java.util.Optional;
import java.util.UUID;

public interface CartaoRepositorio {

  Cartao salvar(Cartao cartao);

  Optional<Cartao> buscarPorId(UUID id);

  boolean existePorPortadorEProduto(UUID portadorId, UUID produtoId);
}

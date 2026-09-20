package br.com.rpe.portador.application.port.out;

import java.util.Optional;
import java.util.UUID;

public interface ProdutoClient {

  Optional<ProdutoDto> buscarPorId(UUID produtoId);
}

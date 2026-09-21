package br.com.rpe.portador.application.port.out;

import java.util.Optional;
import java.util.UUID;

public interface CartaoClient {

  Optional<CartaoDto> buscarPorPortadorId(UUID portadorId);

  /** Falha registrada pelo Cartão para a emissão do portador (issue #117); vazio se não houve. */
  Optional<FalhaEmissaoDto> buscarFalhaEmissao(UUID portadorId);
}

package br.com.rpe.portador.application.port.out;

import java.util.Optional;
import java.util.UUID;

public interface CartaoClient {

  Optional<CartaoDto> buscarPorPortadorId(UUID portadorId);
}

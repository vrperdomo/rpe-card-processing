package br.com.rpe.portador.application.port.out;

import br.com.rpe.portador.domain.Portador;
import java.util.Optional;
import java.util.UUID;

public interface PortadorRepositorio {

  Portador salvar(Portador portador);

  Optional<Portador> buscarPorId(UUID id);

  boolean existePorCpf(String cpf);
}

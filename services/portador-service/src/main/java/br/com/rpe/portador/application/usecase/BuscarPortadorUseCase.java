package br.com.rpe.portador.application.usecase;

import br.com.rpe.portador.application.port.out.PortadorRepositorio;
import br.com.rpe.portador.domain.Portador;
import br.com.rpe.portador.domain.exception.RecursoNaoEncontradoException;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class BuscarPortadorUseCase {

  private final PortadorRepositorio portadorRepositorio;

  public BuscarPortadorUseCase(PortadorRepositorio portadorRepositorio) {
    this.portadorRepositorio = portadorRepositorio;
  }

  @Transactional(readOnly = true)
  public Portador executar(UUID id) {
    return portadorRepositorio
        .buscarPorId(id)
        .orElseThrow(
            () -> new RecursoNaoEncontradoException("Portador %s não encontrado".formatted(id)));
  }
}

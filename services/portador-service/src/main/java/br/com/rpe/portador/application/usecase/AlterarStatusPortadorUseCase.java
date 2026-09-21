package br.com.rpe.portador.application.usecase;

import br.com.rpe.portador.application.port.out.PortadorRepositorio;
import br.com.rpe.portador.application.seguranca.Solicitante;
import br.com.rpe.portador.domain.Portador;
import br.com.rpe.portador.domain.StatusPortador;
import java.time.Clock;
import java.time.Instant;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AlterarStatusPortadorUseCase {

  private final AcessoAoPortador acessoAoPortador;
  private final PortadorRepositorio portadorRepositorio;
  private final Clock clock;

  public AlterarStatusPortadorUseCase(
      AcessoAoPortador acessoAoPortador, PortadorRepositorio portadorRepositorio, Clock clock) {
    this.acessoAoPortador = acessoAoPortador;
    this.portadorRepositorio = portadorRepositorio;
    this.clock = clock;
  }

  @Transactional
  public Portador executar(UUID id, StatusPortador novoStatus, Solicitante solicitante) {
    Portador portador = acessoAoPortador.obter(id, solicitante);
    Instant agora = Instant.now(clock);
    switch (novoStatus) {
      case ATIVO -> portador.ativar(agora);
      case BLOQUEADO -> portador.bloquear(agora);
      case CANCELADO -> portador.cancelar(agora);
    }
    return portadorRepositorio.salvar(portador);
  }
}

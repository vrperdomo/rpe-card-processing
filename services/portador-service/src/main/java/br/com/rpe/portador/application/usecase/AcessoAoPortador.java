package br.com.rpe.portador.application.usecase;

import br.com.rpe.portador.application.port.out.PortadorRepositorio;
import br.com.rpe.portador.application.seguranca.Solicitante;
import br.com.rpe.portador.domain.Portador;
import br.com.rpe.portador.domain.exception.RecursoNaoEncontradoException;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Ponto único de leitura de um portador em nome de um solicitante (ADR-009, A01). Quem não é o dono
 * recebe o mesmo {@link RecursoNaoEncontradoException} de um id inexistente: responder 403
 * revelaria que o id existe e permitiria enumerar portadores alheios. A tentativa fica em log.
 */
@Component
public class AcessoAoPortador {

  private static final Logger log = LoggerFactory.getLogger(AcessoAoPortador.class);

  private final PortadorRepositorio portadorRepositorio;

  public AcessoAoPortador(PortadorRepositorio portadorRepositorio) {
    this.portadorRepositorio = portadorRepositorio;
  }

  public Portador obter(UUID id, Solicitante solicitante) {
    Portador portador = portadorRepositorio.buscarPorId(id).orElseThrow(() -> naoEncontrado(id));
    if (!solicitante.podeAcessar(portador.getCriadoPor())) {
      log.warn(
          "Acesso negado por posse do recurso: portadorId={} solicitante={}", id, solicitante.id());
      throw naoEncontrado(id);
    }
    return portador;
  }

  private static RecursoNaoEncontradoException naoEncontrado(UUID id) {
    return new RecursoNaoEncontradoException("Portador %s não encontrado".formatted(id));
  }
}

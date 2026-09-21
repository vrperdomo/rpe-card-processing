package br.com.rpe.portador.application.usecase;

import br.com.rpe.portador.application.seguranca.Solicitante;
import br.com.rpe.portador.domain.Portador;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class BuscarPortadorUseCase {

  private final AcessoAoPortador acessoAoPortador;

  public BuscarPortadorUseCase(AcessoAoPortador acessoAoPortador) {
    this.acessoAoPortador = acessoAoPortador;
  }

  @Transactional(readOnly = true)
  public Portador executar(UUID id, Solicitante solicitante) {
    return acessoAoPortador.obter(id, solicitante);
  }
}

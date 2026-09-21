package br.com.rpe.cartao.application.usecase;

import br.com.rpe.cartao.application.port.out.EmissaoFalhaRepositorio;
import br.com.rpe.cartao.application.seguranca.Solicitante;
import br.com.rpe.cartao.domain.EmissaoFalha;
import br.com.rpe.cartao.domain.exception.RecursoNaoEncontradoException;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Consulta a falha de emissão de um portador (issue #117). Segue a regra de posse do cartão
 * (ADR-009, A01): quem não é dono recebe o mesmo 404 de "nenhuma falha registrada".
 */
@Service
public class BuscarFalhaEmissaoUseCase {

  private static final Logger log = LoggerFactory.getLogger(BuscarFalhaEmissaoUseCase.class);

  private final EmissaoFalhaRepositorio emissaoFalhaRepositorio;

  public BuscarFalhaEmissaoUseCase(EmissaoFalhaRepositorio emissaoFalhaRepositorio) {
    this.emissaoFalhaRepositorio = emissaoFalhaRepositorio;
  }

  @Transactional(readOnly = true)
  public EmissaoFalha executar(UUID portadorId, Solicitante solicitante) {
    EmissaoFalha falha =
        emissaoFalhaRepositorio
            .buscarPorPortadorId(portadorId)
            .orElseThrow(() -> naoEncontrada(portadorId));
    if (!solicitante.podeAcessar(falha.criadoPor())) {
      log.warn(
          "Acesso negado por posse do recurso: falhaEmissaoDoPortador={} solicitante={}",
          portadorId,
          solicitante.id());
      throw naoEncontrada(portadorId);
    }
    return falha;
  }

  private static RecursoNaoEncontradoException naoEncontrada(UUID portadorId) {
    return new RecursoNaoEncontradoException(
        "Nenhuma falha de emissão registrada para o portador %s".formatted(portadorId));
  }
}

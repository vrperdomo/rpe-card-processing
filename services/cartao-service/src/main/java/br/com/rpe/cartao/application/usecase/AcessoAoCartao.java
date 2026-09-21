package br.com.rpe.cartao.application.usecase;

import br.com.rpe.cartao.application.port.out.CartaoRepositorio;
import br.com.rpe.cartao.application.seguranca.Solicitante;
import br.com.rpe.cartao.domain.Cartao;
import br.com.rpe.cartao.domain.exception.RecursoNaoEncontradoException;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Ponto único de leitura de um cartão em nome de um solicitante (ADR-009, A01). Quem não é o dono
 * recebe o mesmo {@link RecursoNaoEncontradoException} de um id inexistente: responder 403
 * revelaria que o id existe e permitiria enumerar cartões alheios. A tentativa fica em log.
 */
@Component
public class AcessoAoCartao {

  private static final Logger log = LoggerFactory.getLogger(AcessoAoCartao.class);

  private final CartaoRepositorio cartaoRepositorio;

  public AcessoAoCartao(CartaoRepositorio cartaoRepositorio) {
    this.cartaoRepositorio = cartaoRepositorio;
  }

  public Cartao obter(UUID id, Solicitante solicitante) {
    Cartao cartao = cartaoRepositorio.buscarPorId(id).orElseThrow(() -> naoEncontrado(id));
    if (!solicitante.podeAcessar(cartao.getCriadoPor())) {
      log.warn(
          "Acesso negado por posse do recurso: cartaoId={} solicitante={}", id, solicitante.id());
      throw naoEncontrado(id);
    }
    return cartao;
  }

  private static RecursoNaoEncontradoException naoEncontrado(UUID id) {
    return new RecursoNaoEncontradoException("Cartão %s não encontrado".formatted(id));
  }
}

package br.com.rpe.cartao.application.usecase;

import br.com.rpe.cartao.application.port.out.CartaoRepositorio;
import br.com.rpe.cartao.domain.Cartao;
import br.com.rpe.cartao.domain.StatusCartao;
import br.com.rpe.cartao.domain.exception.RecursoNaoEncontradoException;
import java.time.Clock;
import java.time.Instant;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AlterarStatusCartaoUseCase {

  private final CartaoRepositorio cartaoRepositorio;
  private final Clock clock;

  public AlterarStatusCartaoUseCase(CartaoRepositorio cartaoRepositorio, Clock clock) {
    this.cartaoRepositorio = cartaoRepositorio;
    this.clock = clock;
  }

  @Transactional
  public Cartao executar(UUID id, StatusCartao novoStatus) {
    Cartao cartao =
        cartaoRepositorio
            .buscarPorId(id)
            .orElseThrow(
                () -> new RecursoNaoEncontradoException("Cartão %s não encontrado".formatted(id)));
    Instant agora = Instant.now(clock);
    switch (novoStatus) {
      case ATIVO -> cartao.ativar(agora);
      case BLOQUEADO -> cartao.bloquear(agora);
      case CANCELADO -> cartao.cancelar(agora);
    }
    return cartaoRepositorio.salvar(cartao);
  }
}

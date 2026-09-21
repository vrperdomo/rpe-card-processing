package br.com.rpe.cartao.application.usecase;

import br.com.rpe.cartao.application.port.out.CartaoRepositorio;
import br.com.rpe.cartao.application.seguranca.Solicitante;
import br.com.rpe.cartao.domain.Cartao;
import br.com.rpe.cartao.domain.StatusCartao;
import java.time.Clock;
import java.time.Instant;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AlterarStatusCartaoUseCase {

  private final AcessoAoCartao acessoAoCartao;
  private final CartaoRepositorio cartaoRepositorio;
  private final Clock clock;

  public AlterarStatusCartaoUseCase(
      AcessoAoCartao acessoAoCartao, CartaoRepositorio cartaoRepositorio, Clock clock) {
    this.acessoAoCartao = acessoAoCartao;
    this.cartaoRepositorio = cartaoRepositorio;
    this.clock = clock;
  }

  @Transactional
  public Cartao executar(UUID id, StatusCartao novoStatus, Solicitante solicitante) {
    Cartao cartao = acessoAoCartao.obter(id, solicitante);
    Instant agora = Instant.now(clock);
    switch (novoStatus) {
      case ATIVO -> cartao.ativar(agora);
      case BLOQUEADO -> cartao.bloquear(agora);
      case CANCELADO -> cartao.cancelar(agora);
    }
    return cartaoRepositorio.salvar(cartao);
  }
}

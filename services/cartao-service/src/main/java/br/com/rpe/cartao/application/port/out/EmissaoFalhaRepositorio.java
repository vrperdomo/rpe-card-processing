package br.com.rpe.cartao.application.port.out;

import br.com.rpe.cartao.domain.EmissaoFalha;
import java.util.Optional;
import java.util.UUID;

public interface EmissaoFalhaRepositorio {

  /** Grava a falha; uma falha anterior do mesmo portador é substituída. */
  void registrar(EmissaoFalha falha);

  Optional<EmissaoFalha> buscarPorPortadorId(UUID portadorId);

  void removerPorPortadorId(UUID portadorId);
}

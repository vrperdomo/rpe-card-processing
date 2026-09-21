package br.com.rpe.portador.application.port.out;

import br.com.rpe.portador.domain.exception.LimiteTentativasExcedidoException;

/**
 * Controle de tentativas de login por origem (ADR-009, OWASP A04/A07).
 *
 * <p>A tentativa é reservada <b>antes</b> de a senha ser verificada, de forma atômica com a
 * checagem do limite. Se a contagem só fosse feita depois da falha, uma rajada de requisições
 * paralelas passaria toda pela checagem antes de qualquer falha ser registrada. Login bem-sucedido
 * zera a contagem da origem, então um usuário legítimo não é penalizado pelas próprias tentativas
 * corretas nem por origens alheias.
 */
public interface ControleTentativasLogin {

  /**
   * @throws LimiteTentativasExcedidoException se a origem já usou todas as tentativas da janela
   */
  void registrarTentativa(String origem);

  void registrarSucesso(String origem);
}

package br.com.rpe.portador.application.usecase;

import br.com.rpe.portador.application.port.out.CartaoClient;
import br.com.rpe.portador.application.port.out.CartaoDto;
import br.com.rpe.portador.application.port.out.ProdutoClient;
import br.com.rpe.portador.application.port.out.ProdutoDto;
import br.com.rpe.portador.application.seguranca.Solicitante;
import br.com.rpe.portador.application.usecase.PortadorCompleto.StatusEmissao;
import br.com.rpe.portador.domain.Portador;
import br.com.rpe.portador.domain.exception.DependenciaIndisponivelException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Consulta agregada (PO-09, PRD 8.4): Portador → Cartão → Produto, em sequência (decisão de
 * replanejamento 20/09, CLAUDE.md seção 3.1 — paralelização fica para depois). Indisponibilidade do
 * Cartão ou do Produto não derruba a consulta: vira um aviso e a resposta continua 200: 503 é
 * reservado para quando o próprio Portador não puder ser lido (CLAUDE.md "Decisões já tomadas").
 */
@Service
public class BuscarPortadorCompletoUseCase {

  private static final Logger log = LoggerFactory.getLogger(BuscarPortadorCompletoUseCase.class);

  private final AcessoAoPortador acessoAoPortador;
  private final CartaoClient cartaoClient;
  private final ProdutoClient produtoClient;

  public BuscarPortadorCompletoUseCase(
      AcessoAoPortador acessoAoPortador, CartaoClient cartaoClient, ProdutoClient produtoClient) {
    this.acessoAoPortador = acessoAoPortador;
    this.cartaoClient = cartaoClient;
    this.produtoClient = produtoClient;
  }

  // A posse é verificada ANTES de chamar Cartão e Produto: quem não é o dono nunca dispara as
  // chamadas remotas (o Cartão é consultado com o token de serviço, que ignora a posse).
  @Transactional(readOnly = true)
  public PortadorCompleto executar(UUID portadorId, Solicitante solicitante) {
    Portador portador = acessoAoPortador.obter(portadorId, solicitante);

    List<String> avisos = new ArrayList<>();

    Optional<CartaoDto> cartao = Optional.empty();
    boolean cartaoIndisponivel = false;
    try {
      cartao = cartaoClient.buscarPorPortadorId(portadorId);
    } catch (DependenciaIndisponivelException ex) {
      cartaoIndisponivel = true;
      avisos.add("Cartão indisponível no momento");
      log.warn("Cartão Service indisponível ao montar consulta agregada de {}", portadorId);
    }

    Optional<ProdutoDto> produto = Optional.empty();
    try {
      produto = produtoClient.buscarPorId(portador.getProdutoId());
    } catch (DependenciaIndisponivelException ex) {
      avisos.add("Produto indisponível no momento");
      log.warn("Produto Service indisponível ao montar consulta agregada de {}", portadorId);
    }

    StatusEmissao emissao =
        cartaoIndisponivel
            ? StatusEmissao.DESCONHECIDA
            : cartao.isPresent() ? StatusEmissao.CONCLUIDA : StatusEmissao.PENDENTE;

    return new PortadorCompleto(portador, cartao, produto, emissao, avisos);
  }
}

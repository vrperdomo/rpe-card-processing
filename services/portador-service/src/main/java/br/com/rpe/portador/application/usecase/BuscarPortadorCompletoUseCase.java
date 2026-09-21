package br.com.rpe.portador.application.usecase;

import br.com.rpe.portador.application.port.out.CartaoClient;
import br.com.rpe.portador.application.port.out.CartaoDto;
import br.com.rpe.portador.application.port.out.FalhaEmissaoDto;
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
    EstadoDoCartao cartao = consultarCartao(portadorId, avisos);
    Optional<ProdutoDto> produto = consultarProduto(portador, avisos);
    return new PortadorCompleto(
        portador, cartao.cartao(), produto, cartao.falha(), cartao.emissao(), avisos);
  }

  // Só pergunta pela falha quando NÃO há cartão: o caminho feliz continua com uma única chamada ao
  // Cartão. Falhar em qualquer das duas consultas deixa o estado DESCONHECIDA: não dá para afirmar
  // PENDENTE nem FALHOU sem saber o que o Cartão registrou.
  private EstadoDoCartao consultarCartao(UUID portadorId, List<String> avisos) {
    try {
      Optional<CartaoDto> cartao = cartaoClient.buscarPorPortadorId(portadorId);
      if (cartao.isPresent()) {
        return new EstadoDoCartao(cartao, Optional.empty(), StatusEmissao.CONCLUIDA);
      }
      Optional<FalhaEmissaoDto> falha = cartaoClient.buscarFalhaEmissao(portadorId);
      return new EstadoDoCartao(
          Optional.empty(),
          falha,
          falha.isPresent() ? StatusEmissao.FALHOU : StatusEmissao.PENDENTE);
    } catch (DependenciaIndisponivelException ex) {
      avisos.add("Cartão indisponível no momento");
      log.warn("Cartão Service indisponível ao montar consulta agregada de {}", portadorId);
      return new EstadoDoCartao(Optional.empty(), Optional.empty(), StatusEmissao.DESCONHECIDA);
    }
  }

  private Optional<ProdutoDto> consultarProduto(Portador portador, List<String> avisos) {
    try {
      return produtoClient.buscarPorId(portador.getProdutoId());
    } catch (DependenciaIndisponivelException ex) {
      avisos.add("Produto indisponível no momento");
      log.warn("Produto Service indisponível ao montar consulta agregada de {}", portador.getId());
      return Optional.empty();
    }
  }

  private record EstadoDoCartao(
      Optional<CartaoDto> cartao, Optional<FalhaEmissaoDto> falha, StatusEmissao emissao) {}
}

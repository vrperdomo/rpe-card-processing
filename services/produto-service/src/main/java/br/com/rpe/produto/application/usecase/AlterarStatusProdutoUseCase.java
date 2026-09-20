package br.com.rpe.produto.application.usecase;

import br.com.rpe.produto.application.evento.ProdutoAtualizadoEvento;
import br.com.rpe.produto.application.port.out.ProdutoRepositorio;
import br.com.rpe.produto.domain.Produto;
import br.com.rpe.produto.domain.StatusProduto;
import br.com.rpe.produto.domain.exception.RecursoNaoEncontradoException;
import br.com.rpe.produto.domain.exception.RegraNegocioException;
import java.time.Clock;
import java.time.Instant;
import java.util.UUID;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AlterarStatusProdutoUseCase {

  private final ProdutoRepositorio produtoRepositorio;
  private final ApplicationEventPublisher eventPublisher;
  private final Clock clock;

  public AlterarStatusProdutoUseCase(
      ProdutoRepositorio produtoRepositorio,
      ApplicationEventPublisher eventPublisher,
      Clock clock) {
    this.produtoRepositorio = produtoRepositorio;
    this.eventPublisher = eventPublisher;
    this.clock = clock;
  }

  @Transactional
  public Produto executar(UUID id, StatusProduto novoStatus, String correlationId) {
    if (novoStatus != StatusProduto.CANCELADO) {
      throw new RegraNegocioException(
          "Transição de status para %s não é permitida".formatted(novoStatus));
    }
    Produto produto =
        produtoRepositorio
            .buscarPorId(id)
            .orElseThrow(
                () -> new RecursoNaoEncontradoException("Produto %s não encontrado".formatted(id)));
    produto.cancelar(Instant.now(clock));
    Produto salvo = produtoRepositorio.salvar(produto);
    eventPublisher.publishEvent(new ProdutoAtualizadoEvento(salvo.getId(), correlationId));
    return salvo;
  }
}

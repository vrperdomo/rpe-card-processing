package br.com.rpe.cartao.application.usecase;

import br.com.rpe.cartao.application.port.out.ProdutoCacheEvictor;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class EvictarCacheProdutoUseCase {

  private final ProdutoCacheEvictor evictor;

  public EvictarCacheProdutoUseCase(ProdutoCacheEvictor evictor) {
    this.evictor = evictor;
  }

  public void executar(UUID produtoId) {
    evictor.evict(produtoId);
  }
}

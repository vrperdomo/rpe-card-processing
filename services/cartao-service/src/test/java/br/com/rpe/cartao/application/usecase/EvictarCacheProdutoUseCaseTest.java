package br.com.rpe.cartao.application.usecase;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import br.com.rpe.cartao.application.port.out.ProdutoCacheEvictor;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class EvictarCacheProdutoUseCaseTest {

  private final ProdutoCacheEvictor evictor = mock(ProdutoCacheEvictor.class);
  private final EvictarCacheProdutoUseCase useCase = new EvictarCacheProdutoUseCase(evictor);

  @Test
  void deveDelegarEviccaoParaOEvictorDeCache() {
    UUID produtoId = UUID.randomUUID();

    useCase.executar(produtoId);

    verify(evictor).evict(produtoId);
  }
}

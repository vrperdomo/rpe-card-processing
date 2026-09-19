package br.com.rpe.cartao.application.port.out;

import java.util.UUID;

public interface ProdutoCacheEvictor {

  void evict(UUID produtoId);
}

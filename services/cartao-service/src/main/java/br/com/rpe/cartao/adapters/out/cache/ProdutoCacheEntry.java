package br.com.rpe.cartao.adapters.out.cache;

import br.com.rpe.cartao.application.port.out.ProdutoDto;

// produto nulo representa cache negativo (produto não encontrado no Produto Service),
// distinto de chave ausente (nunca cacheado).
public record ProdutoCacheEntry(ProdutoDto produto) {}

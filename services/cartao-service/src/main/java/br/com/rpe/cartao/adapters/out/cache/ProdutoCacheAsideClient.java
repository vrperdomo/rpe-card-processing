package br.com.rpe.cartao.adapters.out.cache;

import br.com.rpe.cartao.adapters.out.http.ProdutoHttpClient;
import br.com.rpe.cartao.application.port.out.ProdutoCacheEvictor;
import br.com.rpe.cartao.application.port.out.ProdutoClient;
import br.com.rpe.cartao.application.port.out.ProdutoDto;
import br.com.rpe.cartao.config.ProdutoCacheProperties;
import io.micrometer.core.instrument.MeterRegistry;
import java.util.Optional;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

@Component
public class ProdutoCacheAsideClient implements ProdutoClient, ProdutoCacheEvictor {

  private static final Logger log = LoggerFactory.getLogger(ProdutoCacheAsideClient.class);
  private static final String PREFIXO_CHAVE = "produto:v1:";

  private final ProdutoHttpClient delegate;
  private final RedisTemplate<String, ProdutoCacheEntry> redisTemplate;
  private final ProdutoCacheProperties properties;
  private final MeterRegistry meterRegistry;

  public ProdutoCacheAsideClient(
      ProdutoHttpClient delegate,
      RedisTemplate<String, ProdutoCacheEntry> redisTemplate,
      ProdutoCacheProperties properties,
      MeterRegistry meterRegistry) {
    this.delegate = delegate;
    this.redisTemplate = redisTemplate;
    this.properties = properties;
    this.meterRegistry = meterRegistry;
  }

  @Override
  public Optional<ProdutoDto> buscarPorId(UUID produtoId) {
    String chave = PREFIXO_CHAVE + produtoId;

    Optional<ProdutoCacheEntry> cacheado = lerDoCache(chave);
    if (cacheado.isPresent()) {
      meterRegistry.counter("produto.cache.hit").increment();
      return Optional.ofNullable(cacheado.get().produto());
    }

    meterRegistry.counter("produto.cache.miss").increment();
    Optional<ProdutoDto> resultado = delegate.buscarPorId(produtoId);
    escreverNoCache(chave, resultado);
    return resultado;
  }

  private Optional<ProdutoCacheEntry> lerDoCache(String chave) {
    try {
      return Optional.ofNullable(redisTemplate.opsForValue().get(chave));
    } catch (DataAccessException ex) {
      log.warn("Redis indisponível ao ler cache de produto, seguindo sem cache", ex);
      return Optional.empty();
    }
  }

  @Override
  public void evict(UUID produtoId) {
    String chave = PREFIXO_CHAVE + produtoId;
    try {
      redisTemplate.delete(chave);
    } catch (DataAccessException ex) {
      log.warn(
          "Redis indisponível ao evictar cache de produto {}, TTL cobre a janela", produtoId, ex);
    }
  }

  private void escreverNoCache(String chave, Optional<ProdutoDto> resultado) {
    try {
      var ttl = resultado.isPresent() ? properties.ttl() : properties.ttlNegativo();
      redisTemplate.opsForValue().set(chave, new ProdutoCacheEntry(resultado.orElse(null)), ttl);
    } catch (DataAccessException ex) {
      log.warn("Redis indisponível ao gravar cache de produto, seguindo sem cache", ex);
    }
  }
}

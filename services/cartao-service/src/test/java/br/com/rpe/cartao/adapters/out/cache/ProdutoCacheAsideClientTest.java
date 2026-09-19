package br.com.rpe.cartao.adapters.out.cache;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import br.com.rpe.cartao.adapters.out.http.ProdutoHttpClient;
import br.com.rpe.cartao.application.port.out.ProdutoDto;
import br.com.rpe.cartao.application.port.out.StatusProdutoExterno;
import br.com.rpe.cartao.config.ProdutoCacheProperties;
import br.com.rpe.cartao.domain.exception.DependenciaIndisponivelException;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.time.Duration;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceClientConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@Testcontainers
class ProdutoCacheAsideClientTest {

  @Container @ServiceConnection
  static final GenericContainer<?> REDIS =
      new GenericContainer<>(DockerImageName.parse("redis:7-alpine")).withExposedPorts(6379);

  @MockitoBean private ProdutoHttpClient delegate;

  @Autowired private ProdutoCacheAsideClient cacheAsideClient;
  @Autowired private RedisTemplate<String, ProdutoCacheEntry> redisTemplate;

  @AfterEach
  void limparRedis() {
    redisTemplate.execute(
        (org.springframework.data.redis.core.RedisCallback<Void>)
            connection -> {
              connection.serverCommands().flushAll();
              return null;
            });
  }

  private String chave(UUID id) {
    return "produto:v1:" + id;
  }

  @Test
  void devePopularCacheAoBuscarPelaPrimeiraVez() {
    UUID id = UUID.randomUUID();
    ProdutoDto dto = new ProdutoDto(id, "Gold", "GOLD", "453201", StatusProdutoExterno.ATIVO);
    when(delegate.buscarPorId(id)).thenReturn(Optional.of(dto));

    Optional<ProdutoDto> resultado = cacheAsideClient.buscarPorId(id);

    assertThat(resultado).contains(dto);
    assertThat(redisTemplate.hasKey(chave(id))).isTrue();
  }

  @Test
  void deveRetornarDoCacheSemChamarDelegateNaSegundaBusca() {
    UUID id = UUID.randomUUID();
    ProdutoDto dto = new ProdutoDto(id, "Gold", "GOLD", "453201", StatusProdutoExterno.ATIVO);
    when(delegate.buscarPorId(id)).thenReturn(Optional.of(dto));

    cacheAsideClient.buscarPorId(id);
    Optional<ProdutoDto> segunda = cacheAsideClient.buscarPorId(id);

    assertThat(segunda).contains(dto);
    verify(delegate, times(1)).buscarPorId(id);
  }

  @Test
  void deveCachearNegativoQuandoProdutoNaoEncontradoSemChamarDelegateDeNovo() {
    UUID id = UUID.randomUUID();
    when(delegate.buscarPorId(id)).thenReturn(Optional.empty());

    cacheAsideClient.buscarPorId(id);
    Optional<ProdutoDto> segunda = cacheAsideClient.buscarPorId(id);

    assertThat(segunda).isEmpty();
    verify(delegate, times(1)).buscarPorId(id);
  }

  @Test
  void naoDeveCachearQuandoDelegateSinalizaIndisponibilidade() {
    UUID id = UUID.randomUUID();
    when(delegate.buscarPorId(id))
        .thenThrow(new DependenciaIndisponivelException("indisponível", Duration.ofSeconds(10)));

    assertThatThrownBy(() -> cacheAsideClient.buscarPorId(id))
        .isInstanceOf(DependenciaIndisponivelException.class);
    assertThat(redisTemplate.hasKey(chave(id))).isFalse();
  }

  @Test
  void deveSeguirSemCacheQuandoRedisIndisponivel() {
    UUID id = UUID.randomUUID();
    ProdutoDto dto = new ProdutoDto(id, "Gold", "GOLD", "453201", StatusProdutoExterno.ATIVO);
    ProdutoHttpClient delegateIsolado = mock(ProdutoHttpClient.class);
    when(delegateIsolado.buscarPorId(id)).thenReturn(Optional.of(dto));

    LettuceClientConfiguration clientConfig =
        LettuceClientConfiguration.builder().commandTimeout(Duration.ofMillis(300)).build();
    LettuceConnectionFactory conexaoQuebrada =
        new LettuceConnectionFactory(
            new RedisStandaloneConfiguration("localhost", 1), clientConfig);
    conexaoQuebrada.afterPropertiesSet();
    RedisTemplate<String, ProdutoCacheEntry> templateQuebrado = new RedisTemplate<>();
    templateQuebrado.setConnectionFactory(conexaoQuebrada);
    templateQuebrado.setKeySerializer(new StringRedisSerializer());
    templateQuebrado.setValueSerializer(new GenericJackson2JsonRedisSerializer());
    templateQuebrado.afterPropertiesSet();

    ProdutoCacheAsideClient clienteIsolado =
        new ProdutoCacheAsideClient(
            delegateIsolado,
            templateQuebrado,
            new ProdutoCacheProperties(Duration.ofMinutes(10), Duration.ofSeconds(60)),
            new SimpleMeterRegistry());

    Optional<ProdutoDto> resultado = clienteIsolado.buscarPorId(id);

    assertThat(resultado).contains(dto);
    verify(delegateIsolado).buscarPorId(id);
    conexaoQuebrada.destroy();
  }

  @Test
  void deveAplicarTtlMenorParaCacheNegativo() {
    UUID id = UUID.randomUUID();
    when(delegate.buscarPorId(id)).thenReturn(Optional.empty());

    cacheAsideClient.buscarPorId(id);

    Long ttl = redisTemplate.getExpire(chave(id), TimeUnit.SECONDS);
    assertThat(ttl).isPositive().isLessThanOrEqualTo(60L);
  }
}

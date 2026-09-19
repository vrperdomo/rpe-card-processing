package br.com.rpe.cartao.config;

import br.com.rpe.cartao.adapters.out.cache.ProdutoCacheEntry;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

@Configuration
@EnableConfigurationProperties(ProdutoCacheProperties.class)
public class RedisConfig {

  @Bean
  public RedisTemplate<String, ProdutoCacheEntry> produtoCacheRedisTemplate(
      RedisConnectionFactory connectionFactory) {
    RedisTemplate<String, ProdutoCacheEntry> template = new RedisTemplate<>();
    template.setConnectionFactory(connectionFactory);
    template.setKeySerializer(new StringRedisSerializer());
    template.setValueSerializer(new GenericJackson2JsonRedisSerializer());
    template.afterPropertiesSet();
    return template;
  }
}

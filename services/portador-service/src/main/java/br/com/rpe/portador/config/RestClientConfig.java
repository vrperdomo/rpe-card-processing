package br.com.rpe.portador.config;

import br.com.rpe.portador.adapters.out.http.ProdutoAuthInterceptor;
import java.util.concurrent.Executor;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.http.client.ClientHttpRequestFactoryBuilder;
import org.springframework.boot.http.client.ClientHttpRequestFactorySettings;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.web.client.RestClient;

@Configuration
@EnableConfigurationProperties({ProdutoClientProperties.class, CartaoClientProperties.class})
public class RestClientConfig {

  @Bean
  public RestClient produtoRestClient(
      ProdutoClientProperties properties, ProdutoAuthInterceptor produtoAuthInterceptor) {
    return construirRestClient(
        properties.baseUrl(),
        properties.connectTimeout(),
        properties.readTimeout(),
        produtoAuthInterceptor);
  }

  @Bean
  public Executor produtoClientExecutor() {
    return construirExecutor("produto-client-");
  }

  // Reusa o mesmo interceptor (token de serviço-para-serviço) do client de Produto: o token
  // gerado por EmissorTokenServico é válido para qualquer serviço que valide o mesmo emissor,
  // não é específico de um destino.
  @Bean
  public RestClient cartaoRestClient(
      CartaoClientProperties properties, ProdutoAuthInterceptor produtoAuthInterceptor) {
    return construirRestClient(
        properties.baseUrl(),
        properties.connectTimeout(),
        properties.readTimeout(),
        produtoAuthInterceptor);
  }

  @Bean
  public Executor cartaoClientExecutor() {
    return construirExecutor("cartao-client-");
  }

  private RestClient construirRestClient(
      String baseUrl,
      java.time.Duration connectTimeout,
      java.time.Duration readTimeout,
      ProdutoAuthInterceptor authInterceptor) {
    ClientHttpRequestFactorySettings settings =
        ClientHttpRequestFactorySettings.defaults()
            .withConnectTimeout(connectTimeout)
            .withReadTimeout(readTimeout);
    ClientHttpRequestFactory requestFactory =
        ClientHttpRequestFactoryBuilder.detect().build(settings);
    return RestClient.builder()
        .baseUrl(baseUrl)
        .requestFactory(requestFactory)
        .requestInterceptor(authInterceptor)
        .build();
  }

  private Executor construirExecutor(String prefixoThread) {
    ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
    executor.setCorePoolSize(4);
    executor.setMaxPoolSize(8);
    executor.setQueueCapacity(50);
    executor.setThreadNamePrefix(prefixoThread);
    executor.initialize();
    return executor;
  }
}

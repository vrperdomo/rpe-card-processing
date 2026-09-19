package br.com.rpe.cartao.config;

import io.awspring.cloud.sqs.config.SqsMessageListenerContainerFactory;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.services.sqs.SqsAsyncClient;

/**
 * {@code listener-auto-startup=false} evita que o container tente resolver/criar a fila no startup
 * do contexto — usado pelos testes de fatia que não sobem LocalStack (ex.:
 * ProdutoCacheAsideClientTest, ProdutoHttpClientTest), que senão falhariam ao subir o contexto
 * completo sem SQS disponível.
 */
@Configuration
@EnableConfigurationProperties(MensageriaProperties.class)
public class MensageriaConfig {

  @Bean
  public SqsMessageListenerContainerFactory<Object> defaultSqsListenerContainerFactory(
      SqsAsyncClient sqsAsyncClient, MensageriaProperties properties) {
    return SqsMessageListenerContainerFactory.builder()
        .sqsAsyncClient(sqsAsyncClient)
        .configure(options -> options.autoStartup(properties.listenerAutoStartup()))
        .build();
  }
}

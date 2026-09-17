package br.com.rpe.produto.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.web.config.PageableHandlerMethodArgumentResolverCustomizer;

@Configuration
public class WebConfig {

  private static final int TAMANHO_PAGINA_PADRAO = 20;
  private static final int TAMANHO_PAGINA_MAXIMO = 50;

  @Bean
  public PageableHandlerMethodArgumentResolverCustomizer pageableCustomizer() {
    return resolver -> {
      resolver.setMaxPageSize(TAMANHO_PAGINA_MAXIMO);
      resolver.setFallbackPageable(PageRequest.of(0, TAMANHO_PAGINA_PADRAO));
    };
  }
}

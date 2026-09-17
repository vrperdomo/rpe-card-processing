package br.com.rpe.produto.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

  @Bean
  public OpenAPI produtoServiceOpenApi() {
    return new OpenAPI()
        .info(
            new Info()
                .title("Produto Service")
                .description("Catálogo de produtos de cartão da RPE Card Processing Platform.")
                .version("v1"));
  }
}

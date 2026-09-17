package br.com.rpe.cartao.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

  @Bean
  public OpenAPI cartaoServiceOpenApi() {
    return new OpenAPI()
        .info(
            new Info()
                .title("Cartão Service")
                .description(
                    "Emissão assíncrona e ciclo de vida de cartões (RPE Card Processing Platform).")
                .version("v1"));
  }
}

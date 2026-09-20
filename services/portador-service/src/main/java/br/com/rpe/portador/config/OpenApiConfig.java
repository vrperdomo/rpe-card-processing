package br.com.rpe.portador.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

  @Bean
  public OpenAPI portadorServiceOpenApi() {
    return new OpenAPI()
        .info(
            new Info()
                .title("Portador Service")
                .description(
                    "Cadastro de portadores, autenticação JWT e orquestração de emissão de"
                        + " cartão (RPE Card Processing Platform).")
                .version("v1"));
  }
}

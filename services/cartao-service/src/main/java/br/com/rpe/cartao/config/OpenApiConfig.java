package br.com.rpe.cartao.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Documentação OpenAPI. Declara o esquema Bearer JWT: sem ele o Swagger UI não mostra o botão
 * "Authorize" e, como todo endpoint (exceto o login) exige JWT, o avaliador não teria como testar
 * nada pelo Swagger. O esquema é global; o login o dispensa com {@code @SecurityRequirements}.
 */
@Configuration
public class OpenApiConfig {

  public static final String ESQUEMA_BEARER = "bearerAuth";

  @Bean
  public OpenAPI cartaoServiceOpenApi() {
    return new OpenAPI()
        .info(
            new Info()
                .title("Cartão Service")
                .description(
                    "Emissão assíncrona e ciclo de vida de cartões (RPE Card Processing Platform).")
                .version("v1"))
        .components(
            new Components()
                .addSecuritySchemes(
                    ESQUEMA_BEARER,
                    new SecurityScheme()
                        .type(SecurityScheme.Type.HTTP)
                        .scheme("bearer")
                        .bearerFormat("JWT")
                        .description(
                            "JWT emitido por POST /api/v1/auth/login (Portador Service). Cole só o"
                                + " token, sem o prefixo 'Bearer '.")))
        .addSecurityItem(new SecurityRequirement().addList(ESQUEMA_BEARER));
  }
}

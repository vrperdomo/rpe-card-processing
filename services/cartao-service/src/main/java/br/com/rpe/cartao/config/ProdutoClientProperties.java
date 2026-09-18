package br.com.rpe.cartao.config;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@ConfigurationProperties(prefix = "rpe.cartao.produto-client")
@Validated
public record ProdutoClientProperties(
    @NotBlank String baseUrl, @NotNull Duration connectTimeout, @NotNull Duration readTimeout) {}

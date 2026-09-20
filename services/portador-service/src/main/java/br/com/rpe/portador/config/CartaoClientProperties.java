package br.com.rpe.portador.config;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@ConfigurationProperties(prefix = "rpe.portador.cartao-client")
@Validated
public record CartaoClientProperties(
    @NotBlank String baseUrl, @NotNull Duration connectTimeout, @NotNull Duration readTimeout) {}

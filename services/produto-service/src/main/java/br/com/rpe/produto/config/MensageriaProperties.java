package br.com.rpe.produto.config;

import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@ConfigurationProperties(prefix = "rpe.produto.mensageria")
@Validated
public record MensageriaProperties(@NotBlank String produtoEventosQueue) {}

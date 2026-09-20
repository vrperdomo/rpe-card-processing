package br.com.rpe.cartao.config;

import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@ConfigurationProperties(prefix = "rpe.cartao.mensageria")
@Validated
public record MensageriaProperties(
    @NotBlank String produtoEventosQueue,
    @NotBlank String produtoEventosDlq,
    @NotBlank String cartaoEmissaoQueue,
    @NotBlank String cartaoEmissaoDlq,
    boolean listenerAutoStartup) {}

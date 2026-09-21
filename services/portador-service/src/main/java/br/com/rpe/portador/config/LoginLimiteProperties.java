package br.com.rpe.portador.config;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.validation.annotation.Validated;

// Limite de falhas de login por origem (ADR-009). maxOrigensRastreadas limita a memória: sem teto,
// um atacante variando o IP de origem faria o mapa de contadores crescer sem fim.
@ConfigurationProperties(prefix = "rpe.auth.login-limite")
@Validated
public record LoginLimiteProperties(
    @DefaultValue("5") @Min(1) int maxFalhas,
    @DefaultValue("1m") @NotNull Duration janela,
    @DefaultValue("10000") @Min(1) int maxOrigensRastreadas) {}

package br.com.rpe.portador.config;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@ConfigurationProperties(prefix = "rpe.jwt")
@Validated
public record JwtProperties(
    @NotBlank String secret,
    @NotBlank String issuer,
    @NotBlank String audience,
    @NotNull Duration expiracao) {}

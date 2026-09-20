package br.com.rpe.portador.config;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@ConfigurationProperties(prefix = "rpe.portador.outbox.relay")
@Validated
public record OutboxRelayProperties(
    @NotBlank String fila,
    @Min(1) int lote,
    @NotNull Duration intervalo,
    @Min(1) int maxTentativas,
    @NotNull Duration backoffBase,
    boolean ativo) {}

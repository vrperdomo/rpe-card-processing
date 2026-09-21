package br.com.rpe.portador.adapters.in.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

public record LoginRequest(
    @Schema(example = "admin", description = "Usuário seed de desenvolvimento") @NotBlank
        String username,
    @Schema(example = "admin123", description = "Senha do usuário seed de desenvolvimento")
        @NotBlank
        String password) {}

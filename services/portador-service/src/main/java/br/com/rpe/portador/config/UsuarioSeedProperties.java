package br.com.rpe.portador.config;

import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

// Usuário técnico único para login (CLAUDE.md secao 6.7): sem cadastro de usuários nesta fase,
// apenas ambiente local/demo, documentado no .env.example.
@ConfigurationProperties(prefix = "rpe.auth.usuario-seed")
@Validated
public record UsuarioSeedProperties(@NotBlank String username, @NotBlank String passwordHash) {}

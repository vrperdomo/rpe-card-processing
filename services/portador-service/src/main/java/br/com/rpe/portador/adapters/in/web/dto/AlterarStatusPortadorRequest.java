package br.com.rpe.portador.adapters.in.web.dto;

import br.com.rpe.portador.domain.StatusPortador;
import jakarta.validation.constraints.NotNull;

public record AlterarStatusPortadorRequest(@NotNull StatusPortador status) {}

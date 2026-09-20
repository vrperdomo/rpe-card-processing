package br.com.rpe.cartao.adapters.in.web.dto;

import br.com.rpe.cartao.domain.StatusCartao;
import jakarta.validation.constraints.NotNull;

public record AlterarStatusCartaoRequest(@NotNull StatusCartao status) {}

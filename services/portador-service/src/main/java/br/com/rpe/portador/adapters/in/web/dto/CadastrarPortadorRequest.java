package br.com.rpe.portador.adapters.in.web.dto;

import br.com.rpe.portador.adapters.in.web.validation.CpfValido;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import java.time.LocalDate;
import java.util.UUID;

public record CadastrarPortadorRequest(
    @NotBlank String nome,
    @NotBlank @CpfValido String cpf,
    @NotNull @Past LocalDate dataNascimento,
    @NotNull UUID produtoId) {}

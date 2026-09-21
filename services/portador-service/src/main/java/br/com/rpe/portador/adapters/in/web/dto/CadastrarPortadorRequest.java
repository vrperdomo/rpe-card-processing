package br.com.rpe.portador.adapters.in.web.dto;

import br.com.rpe.portador.adapters.in.web.validation.CpfValido;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import java.time.LocalDate;
import java.util.UUID;

public record CadastrarPortadorRequest(
    @Schema(example = "Maria da Silva") @NotBlank String nome,
    @Schema(
            example = "529.982.247-25",
            description = "CPF com dígitos verificadores válidos, com ou sem máscara")
        @NotBlank
        @CpfValido
        String cpf,
    @Schema(
            example = "1990-01-31",
            description = "Data no passado; o portador precisa ter 18 anos ou mais")
        @NotNull
        @Past
        LocalDate dataNascimento,
    @Schema(
            example = "3fa85f64-5717-4562-b3fc-2c963f66afa6",
            description =
                "Id de um produto ATIVO: crie um em POST /api/v1/produtos (Produto Service, porta"
                    + " 8081) e copie o id da resposta")
        @NotNull
        UUID produtoId) {}

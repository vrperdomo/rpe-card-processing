package br.com.rpe.portador.adapters.in.web.dto;

import br.com.rpe.portador.application.usecase.PortadorCompleto.StatusEmissao;
import java.util.List;

public record PortadorCompletoResponse(
    PortadorResponse portador,
    CartaoResumoResponse cartao,
    ProdutoResumoResponse produto,
    StatusEmissao emissao,
    List<String> avisos) {}

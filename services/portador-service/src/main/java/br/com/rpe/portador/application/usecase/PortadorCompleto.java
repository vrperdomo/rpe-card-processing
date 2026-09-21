package br.com.rpe.portador.application.usecase;

import br.com.rpe.portador.application.port.out.CartaoDto;
import br.com.rpe.portador.application.port.out.FalhaEmissaoDto;
import br.com.rpe.portador.application.port.out.ProdutoDto;
import br.com.rpe.portador.domain.Portador;
import java.util.List;
import java.util.Optional;

public record PortadorCompleto(
    Portador portador,
    Optional<CartaoDto> cartao,
    Optional<ProdutoDto> produto,
    Optional<FalhaEmissaoDto> falhaEmissao,
    StatusEmissao emissao,
    List<String> avisos) {

  public enum StatusEmissao {
    CONCLUIDA,
    PENDENTE,
    FALHOU,
    DESCONHECIDA
  }
}

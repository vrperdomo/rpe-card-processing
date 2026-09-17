package br.com.rpe.produto.adapters.in.web.dto;

import java.util.List;
import org.springframework.data.domain.Page;

public record PaginaResponse<T>(
    List<T> conteudo, int pagina, int tamanho, long totalElementos, int totalPaginas) {

  public static <T> PaginaResponse<T> de(Page<T> pagina) {
    return new PaginaResponse<>(
        pagina.getContent(),
        pagina.getNumber(),
        pagina.getSize(),
        pagina.getTotalElements(),
        pagina.getTotalPages());
  }
}

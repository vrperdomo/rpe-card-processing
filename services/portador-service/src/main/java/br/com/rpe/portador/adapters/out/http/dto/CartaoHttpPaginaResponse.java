package br.com.rpe.portador.adapters.out.http.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;

// Espelha só o necessário do PaginaResponse do Cartão Service (CLAUDE.md secao 6.2: paginação nas
// listagens) — o Portador só usa o primeiro item, ja que hoje existe no maximo um cartao por
// portador (um produtoId por cadastro).
@JsonIgnoreProperties(ignoreUnknown = true)
public record CartaoHttpPaginaResponse(List<CartaoHttpResponse> conteudo) {}

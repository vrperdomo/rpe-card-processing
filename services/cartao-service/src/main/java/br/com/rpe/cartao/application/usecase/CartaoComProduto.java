package br.com.rpe.cartao.application.usecase;

import br.com.rpe.cartao.application.port.out.ProdutoDto;
import br.com.rpe.cartao.domain.Cartao;
import java.util.Optional;

// produto vazio quando o Produto Service respondeu mas nao encontrou o recurso (ex.: removido do
// catalogo apos a emissao do cartao) — indisponibilidade da dependencia e sinalizada por
// DependenciaIndisponivelException, nao por um Optional vazio aqui.
public record CartaoComProduto(Cartao cartao, Optional<ProdutoDto> produto) {}

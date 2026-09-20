package br.com.rpe.produto.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import br.com.rpe.produto.application.port.out.ProdutoRepositorio;
import br.com.rpe.produto.domain.CategoriaProduto;
import br.com.rpe.produto.domain.Produto;
import br.com.rpe.produto.domain.StatusProduto;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

@ExtendWith(MockitoExtension.class)
class ListarProdutosUseCaseTest {

  @Mock private ProdutoRepositorio produtoRepositorio;

  private ListarProdutosUseCase useCase;

  @BeforeEach
  void configurar() {
    useCase = new ListarProdutosUseCase(produtoRepositorio);
  }

  @Test
  void deveDelegarListagemParaORepositorioComOsMesmosFiltros() {
    Produto produto =
        Produto.criar(
            "Gold",
            "descricao",
            CategoriaProduto.GOLD,
            "123456",
            Instant.parse("2026-09-17T12:00:00Z"));
    Pageable pageable = PageRequest.of(0, 20);
    Page<Produto> pagina = new PageImpl<>(List.of(produto), pageable, 1);
    when(produtoRepositorio.listar(StatusProduto.ATIVO, pageable)).thenReturn(pagina);

    Page<Produto> resultado = useCase.executar(StatusProduto.ATIVO, pageable);

    assertThat(resultado.getContent()).containsExactly(produto);
  }
}

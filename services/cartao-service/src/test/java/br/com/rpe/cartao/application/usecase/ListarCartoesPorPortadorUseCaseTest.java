package br.com.rpe.cartao.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import br.com.rpe.cartao.application.port.out.CartaoRepositorio;
import br.com.rpe.cartao.application.port.out.ProdutoClient;
import br.com.rpe.cartao.application.port.out.ProdutoDto;
import br.com.rpe.cartao.application.port.out.StatusProdutoExterno;
import br.com.rpe.cartao.domain.Cartao;
import br.com.rpe.cartao.domain.Pan;
import br.com.rpe.cartao.domain.Validade;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

class ListarCartoesPorPortadorUseCaseTest {

  private static final Instant AGORA = Instant.parse("2026-09-20T12:00:00Z");

  private final CartaoRepositorio cartaoRepositorio = mock(CartaoRepositorio.class);
  private final ProdutoClient produtoClient = mock(ProdutoClient.class);
  private final ListarCartoesPorPortadorUseCase useCase =
      new ListarCartoesPorPortadorUseCase(cartaoRepositorio, produtoClient);

  @Test
  void deveListarCartoesDoPortadorComProdutoEmCada() {
    UUID portadorId = UUID.randomUUID();
    UUID produtoId = UUID.randomUUID();
    Cartao cartao =
        Cartao.emitir(
            portadorId,
            produtoId,
            Pan.of("4532015112830366"),
            "VICTOR RODRIGUES",
            Validade.gerar(AGORA),
            AGORA);
    ProdutoDto produtoDto =
        new ProdutoDto(produtoId, "Gold", "GOLD", "453201", StatusProdutoExterno.ATIVO);
    Pageable pageable = PageRequest.of(0, 10);
    when(cartaoRepositorio.buscarPorPortadorId(portadorId, pageable))
        .thenReturn(new PageImpl<>(List.of(cartao), pageable, 1));
    when(produtoClient.buscarPorId(produtoId)).thenReturn(Optional.of(produtoDto));

    var pagina = useCase.executar(portadorId, pageable);

    assertThat(pagina.getContent()).hasSize(1);
    assertThat(pagina.getContent().get(0).cartao()).isEqualTo(cartao);
    assertThat(pagina.getContent().get(0).produto()).contains(produtoDto);
  }
}

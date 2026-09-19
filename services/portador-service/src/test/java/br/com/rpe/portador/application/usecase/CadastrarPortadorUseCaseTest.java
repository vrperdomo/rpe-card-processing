package br.com.rpe.portador.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import br.com.rpe.portador.application.port.out.PortadorRepositorio;
import br.com.rpe.portador.application.port.out.ProdutoClient;
import br.com.rpe.portador.application.port.out.ProdutoDto;
import br.com.rpe.portador.application.port.out.StatusProdutoExterno;
import br.com.rpe.portador.domain.Cpf;
import br.com.rpe.portador.domain.Portador;
import br.com.rpe.portador.domain.StatusPortador;
import br.com.rpe.portador.domain.exception.ConflitoException;
import br.com.rpe.portador.domain.exception.RegraNegocioException;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class CadastrarPortadorUseCaseTest {

  private static final Instant AGORA = Instant.parse("2026-09-19T12:00:00Z");
  private static final Cpf CPF = Cpf.of("52998224725");
  private static final UUID PRODUTO_ID = UUID.randomUUID();
  private static final LocalDate DATA_NASCIMENTO_MAIOR_DE_IDADE = LocalDate.of(2000, 1, 1);

  private final PortadorRepositorio repositorio = mock(PortadorRepositorio.class);
  private final ProdutoClient produtoClient = mock(ProdutoClient.class);
  private final Clock clock = Clock.fixed(AGORA, ZoneOffset.UTC);
  private final CadastrarPortadorUseCase useCase =
      new CadastrarPortadorUseCase(repositorio, produtoClient, clock);

  @Test
  void deveCadastrarQuandoProdutoExisteEstaAtivoECpfNaoEstaEmUso() {
    when(produtoClient.buscarPorId(PRODUTO_ID))
        .thenReturn(Optional.of(new ProdutoDto(PRODUTO_ID, StatusProdutoExterno.ATIVO)));
    when(repositorio.existePorCpf(CPF.valor())).thenReturn(false);
    when(repositorio.salvar(any(Portador.class))).thenAnswer(invocacao -> invocacao.getArgument(0));

    Portador portador = useCase.executar("Victor", CPF, DATA_NASCIMENTO_MAIOR_DE_IDADE, PRODUTO_ID);

    assertThat(portador.getStatus()).isEqualTo(StatusPortador.ATIVO);
    assertThat(portador.getCpf()).isEqualTo(CPF);
    verify(repositorio).salvar(any(Portador.class));
  }

  @Test
  void deveRejeitarQuandoProdutoNaoExiste() {
    when(produtoClient.buscarPorId(PRODUTO_ID)).thenReturn(Optional.empty());

    assertThatThrownBy(
            () -> useCase.executar("Victor", CPF, DATA_NASCIMENTO_MAIOR_DE_IDADE, PRODUTO_ID))
        .isInstanceOf(RegraNegocioException.class);
    verify(repositorio, never()).salvar(any());
  }

  @Test
  void deveRejeitarQuandoProdutoNaoEstaAtivo() {
    when(produtoClient.buscarPorId(PRODUTO_ID))
        .thenReturn(Optional.of(new ProdutoDto(PRODUTO_ID, StatusProdutoExterno.CANCELADO)));

    assertThatThrownBy(
            () -> useCase.executar("Victor", CPF, DATA_NASCIMENTO_MAIOR_DE_IDADE, PRODUTO_ID))
        .isInstanceOf(RegraNegocioException.class);
    verify(repositorio, never()).salvar(any());
  }

  @Test
  void deveRejeitarQuandoCpfJaEstaCadastrado() {
    when(produtoClient.buscarPorId(PRODUTO_ID))
        .thenReturn(Optional.of(new ProdutoDto(PRODUTO_ID, StatusProdutoExterno.ATIVO)));
    when(repositorio.existePorCpf(CPF.valor())).thenReturn(true);

    assertThatThrownBy(
            () -> useCase.executar("Victor", CPF, DATA_NASCIMENTO_MAIOR_DE_IDADE, PRODUTO_ID))
        .isInstanceOf(ConflitoException.class);
    verify(repositorio, never()).salvar(any());
  }

  @Test
  void naoDeveConsultarCpfNemSalvarQuandoProdutoInvalido() {
    when(produtoClient.buscarPorId(PRODUTO_ID)).thenReturn(Optional.empty());

    assertThatThrownBy(
        () -> useCase.executar("Victor", CPF, DATA_NASCIMENTO_MAIOR_DE_IDADE, PRODUTO_ID));

    verify(repositorio, never()).existePorCpf(any());
  }
}

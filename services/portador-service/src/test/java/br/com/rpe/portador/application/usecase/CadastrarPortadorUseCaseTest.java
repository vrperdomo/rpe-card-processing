package br.com.rpe.portador.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import br.com.rpe.portador.application.evento.CartaoEmissaoSolicitadaData;
import br.com.rpe.portador.application.evento.EventoOutbox;
import br.com.rpe.portador.application.port.out.OutboxRepositorio;
import br.com.rpe.portador.application.port.out.PortadorRepositorio;
import br.com.rpe.portador.application.port.out.ProdutoClient;
import br.com.rpe.portador.application.port.out.ProdutoDto;
import br.com.rpe.portador.application.port.out.StatusProdutoExterno;
import br.com.rpe.portador.application.seguranca.Solicitante;
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
import org.mockito.ArgumentCaptor;

class CadastrarPortadorUseCaseTest {

  private static final Instant AGORA = Instant.parse("2026-09-19T12:00:00Z");
  private static final Cpf CPF = Cpf.of("52998224725");
  private static final UUID PRODUTO_ID = UUID.randomUUID();
  private static final LocalDate DATA_NASCIMENTO_MAIOR_DE_IDADE = LocalDate.of(2000, 1, 1);
  private static final String CORRELATION_ID = "corr-123";
  private static final Solicitante SOLICITANTE = Solicitante.deUsuario("admin");

  private final PortadorRepositorio repositorio = mock(PortadorRepositorio.class);
  private final ProdutoClient produtoClient = mock(ProdutoClient.class);
  private final OutboxRepositorio outboxRepositorio = mock(OutboxRepositorio.class);
  private final Clock clock = Clock.fixed(AGORA, ZoneOffset.UTC);
  private final CadastrarPortadorUseCase useCase =
      new CadastrarPortadorUseCase(repositorio, produtoClient, outboxRepositorio, clock);

  private void produtoAtivo() {
    when(produtoClient.buscarPorId(PRODUTO_ID))
        .thenReturn(
            Optional.of(new ProdutoDto(PRODUTO_ID, "Gold", "GOLD", StatusProdutoExterno.ATIVO)));
  }

  @Test
  void deveCadastrarQuandoProdutoExisteEstaAtivoECpfNaoEstaEmUso() {
    produtoAtivo();
    when(repositorio.existePorCpf(CPF.valor())).thenReturn(false);
    when(repositorio.salvar(any(Portador.class))).thenAnswer(invocacao -> invocacao.getArgument(0));

    Portador portador =
        useCase.executar(
            "Victor", CPF, DATA_NASCIMENTO_MAIOR_DE_IDADE, PRODUTO_ID, SOLICITANTE, CORRELATION_ID);

    assertThat(portador.getStatus()).isEqualTo(StatusPortador.ATIVO);
    assertThat(portador.getCpf()).isEqualTo(CPF);
    assertThat(portador.getCriadoPor()).isEqualTo("admin");
    verify(repositorio).salvar(any(Portador.class));
  }

  @Test
  void deveRegistrarEventoDeEmissaoNoOutboxNaMesmaChamada() {
    produtoAtivo();
    when(repositorio.existePorCpf(CPF.valor())).thenReturn(false);
    when(repositorio.salvar(any(Portador.class))).thenAnswer(invocacao -> invocacao.getArgument(0));

    Portador portador =
        useCase.executar(
            "Victor", CPF, DATA_NASCIMENTO_MAIOR_DE_IDADE, PRODUTO_ID, SOLICITANTE, CORRELATION_ID);

    ArgumentCaptor<EventoOutbox> eventoCaptor = ArgumentCaptor.forClass(EventoOutbox.class);
    verify(outboxRepositorio)
        .registrar(eq(portador.getId()), eq("Portador"), eventoCaptor.capture());
    EventoOutbox evento = eventoCaptor.getValue();
    assertThat(evento.eventType()).isEqualTo("CartaoEmissaoSolicitada");
    assertThat(evento.eventVersion()).isEqualTo(1);
    assertThat(evento.correlationId()).isEqualTo(CORRELATION_ID);
    assertThat(evento.occurredAt()).isEqualTo(AGORA);
    assertThat(evento.data()).isInstanceOf(CartaoEmissaoSolicitadaData.class);
    CartaoEmissaoSolicitadaData data = (CartaoEmissaoSolicitadaData) evento.data();
    assertThat(data.portadorId()).isEqualTo(portador.getId());
    assertThat(data.produtoId()).isEqualTo(PRODUTO_ID);
    assertThat(data.nomeImpresso()).isEqualTo("VICTOR");
    assertThat(data.criadoPor()).isEqualTo("admin");
  }

  @Test
  void deveTruncarNomeImpressoEmVinteESeisCaracteres() {
    produtoAtivo();
    when(repositorio.existePorCpf(CPF.valor())).thenReturn(false);
    when(repositorio.salvar(any(Portador.class))).thenAnswer(invocacao -> invocacao.getArgument(0));

    useCase.executar(
        "Victor Rodrigues Perdomo da Silva Junior",
        CPF,
        DATA_NASCIMENTO_MAIOR_DE_IDADE,
        PRODUTO_ID,
        SOLICITANTE,
        CORRELATION_ID);

    ArgumentCaptor<EventoOutbox> eventoCaptor = ArgumentCaptor.forClass(EventoOutbox.class);
    verify(outboxRepositorio).registrar(any(), any(), eventoCaptor.capture());
    CartaoEmissaoSolicitadaData data = (CartaoEmissaoSolicitadaData) eventoCaptor.getValue().data();
    assertThat(data.nomeImpresso()).hasSize(26);
  }

  @Test
  void deveRejeitarQuandoProdutoNaoExiste() {
    when(produtoClient.buscarPorId(PRODUTO_ID)).thenReturn(Optional.empty());

    assertThatThrownBy(
            () ->
                useCase.executar(
                    "Victor",
                    CPF,
                    DATA_NASCIMENTO_MAIOR_DE_IDADE,
                    PRODUTO_ID,
                    SOLICITANTE,
                    CORRELATION_ID))
        .isInstanceOf(RegraNegocioException.class);
    verify(repositorio, never()).salvar(any());
    verify(outboxRepositorio, never()).registrar(any(), any(), any());
  }

  @Test
  void deveRejeitarQuandoProdutoNaoEstaAtivo() {
    when(produtoClient.buscarPorId(PRODUTO_ID))
        .thenReturn(
            Optional.of(
                new ProdutoDto(PRODUTO_ID, "Gold", "GOLD", StatusProdutoExterno.CANCELADO)));

    assertThatThrownBy(
            () ->
                useCase.executar(
                    "Victor",
                    CPF,
                    DATA_NASCIMENTO_MAIOR_DE_IDADE,
                    PRODUTO_ID,
                    SOLICITANTE,
                    CORRELATION_ID))
        .isInstanceOf(RegraNegocioException.class);
    verify(repositorio, never()).salvar(any());
  }

  @Test
  void deveRejeitarQuandoCpfJaEstaCadastrado() {
    produtoAtivo();
    when(repositorio.existePorCpf(CPF.valor())).thenReturn(true);

    assertThatThrownBy(
            () ->
                useCase.executar(
                    "Victor",
                    CPF,
                    DATA_NASCIMENTO_MAIOR_DE_IDADE,
                    PRODUTO_ID,
                    SOLICITANTE,
                    CORRELATION_ID))
        .isInstanceOf(ConflitoException.class);
    verify(repositorio, never()).salvar(any());
    verify(outboxRepositorio, never()).registrar(any(), any(), any());
  }

  @Test
  void naoDeveConsultarCpfNemSalvarQuandoProdutoInvalido() {
    when(produtoClient.buscarPorId(PRODUTO_ID)).thenReturn(Optional.empty());

    assertThatThrownBy(
        () ->
            useCase.executar(
                "Victor",
                CPF,
                DATA_NASCIMENTO_MAIOR_DE_IDADE,
                PRODUTO_ID,
                SOLICITANTE,
                CORRELATION_ID));

    verify(repositorio, never()).existePorCpf(any());
  }
}

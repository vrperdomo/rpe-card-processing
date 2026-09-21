package br.com.rpe.portador.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.com.rpe.portador.domain.exception.RegraNegocioException;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class PortadorTest {

  private static final Instant AGORA = Instant.parse("2026-09-19T12:00:00Z");
  private static final Instant DEPOIS = Instant.parse("2026-09-20T12:00:00Z");
  private static final Cpf CPF = Cpf.of("52998224725");
  private static final UUID PRODUTO_ID = UUID.randomUUID();

  private Portador portadorAtivo() {
    return Portador.cadastrar(
        "Victor Rodrigues", CPF, LocalDate.of(2000, 1, 1), PRODUTO_ID, "admin", AGORA);
  }

  @Test
  void deveCadastrarPortadorComStatusAtivo() {
    Portador portador = portadorAtivo();

    assertThat(portador.getStatus()).isEqualTo(StatusPortador.ATIVO);
    assertThat(portador.getNome()).isEqualTo("Victor Rodrigues");
    assertThat(portador.getCpf()).isEqualTo(CPF);
    assertThat(portador.getProdutoId()).isEqualTo(PRODUTO_ID);
    assertThat(portador.getCriadoEm()).isEqualTo(AGORA);
    assertThat(portador.getAtualizadoEm()).isEqualTo(AGORA);
  }

  @Test
  void deveAceitarPortadorQueCompletouDezoitoAnosHoje() {
    LocalDate dataNascimento = LocalDate.of(2008, 9, 19);

    Portador portador =
        Portador.cadastrar("Fulano", CPF, dataNascimento, PRODUTO_ID, "admin", AGORA);

    assertThat(portador.getStatus()).isEqualTo(StatusPortador.ATIVO);
  }

  @Test
  void deveRejeitarPortadorMenorDeDezoitoAnos() {
    LocalDate dataNascimentoMenorDeIdade = LocalDate.of(2008, 9, 20);

    assertThatThrownBy(
            () ->
                Portador.cadastrar(
                    "Fulano", CPF, dataNascimentoMenorDeIdade, PRODUTO_ID, "admin", AGORA))
        .isInstanceOf(RegraNegocioException.class);
  }

  @Test
  void deveRejeitarNomeEmBranco() {
    assertThatThrownBy(
            () ->
                Portador.cadastrar(" ", CPF, LocalDate.of(2000, 1, 1), PRODUTO_ID, "admin", AGORA))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void deveRejeitarProdutoIdNulo() {
    assertThatThrownBy(
            () -> Portador.cadastrar("Fulano", CPF, LocalDate.of(2000, 1, 1), null, "admin", AGORA))
        .isInstanceOf(NullPointerException.class);
  }

  @Test
  void deveGuardarQuemCadastrouComoDono() {
    assertThat(portadorAtivo().getCriadoPor()).isEqualTo("admin");
  }

  @Test
  void deveRejeitarCriadoPorVazio() {
    assertThatThrownBy(
            () ->
                Portador.cadastrar("Fulano", CPF, LocalDate.of(2000, 1, 1), PRODUTO_ID, " ", AGORA))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(
            () ->
                Portador.cadastrar(
                    "Fulano", CPF, LocalDate.of(2000, 1, 1), PRODUTO_ID, null, AGORA))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void deveBloquearPortadorAtivo() {
    Portador portador = portadorAtivo();

    portador.bloquear(DEPOIS);

    assertThat(portador.getCriadoPor()).isEqualTo("admin");
    assertThat(portador.getStatus()).isEqualTo(StatusPortador.BLOQUEADO);
    assertThat(portador.getAtualizadoEm()).isEqualTo(DEPOIS);
  }

  @Test
  void deveReativarPortadorBloqueado() {
    Portador portador = portadorAtivo();
    portador.bloquear(AGORA);

    portador.ativar(DEPOIS);

    assertThat(portador.getStatus()).isEqualTo(StatusPortador.ATIVO);
    assertThat(portador.getAtualizadoEm()).isEqualTo(DEPOIS);
  }

  @Test
  void deveCancelarPortadorAtivo() {
    Portador portador = portadorAtivo();

    portador.cancelar(DEPOIS);

    assertThat(portador.getStatus()).isEqualTo(StatusPortador.CANCELADO);
  }

  @Test
  void naoDeveReativarPortadorCancelado() {
    Portador portador = portadorAtivo();
    portador.cancelar(AGORA);

    assertThatThrownBy(() -> portador.ativar(DEPOIS)).isInstanceOf(RegraNegocioException.class);
  }

  @Test
  void naoDeveBloquearPortadorCancelado() {
    Portador portador = portadorAtivo();
    portador.cancelar(AGORA);

    assertThatThrownBy(() -> portador.bloquear(DEPOIS)).isInstanceOf(RegraNegocioException.class);
  }

  @Test
  void naoDeveBloquearPortadorJaBloqueado() {
    Portador portador = portadorAtivo();
    portador.bloquear(AGORA);

    assertThatThrownBy(() -> portador.bloquear(DEPOIS)).isInstanceOf(RegraNegocioException.class);
  }

  @Test
  void naoDeveCancelarPortadorJaCancelado() {
    Portador portador = portadorAtivo();
    portador.cancelar(AGORA);

    assertThatThrownBy(() -> portador.cancelar(DEPOIS)).isInstanceOf(RegraNegocioException.class);
  }

  @Test
  void doisPortadoresComMesmoIdDevemSerIguais() {
    UUID id = UUID.randomUUID();
    Portador primeiro =
        Portador.reconstituir(
            id,
            "Victor",
            CPF,
            LocalDate.of(2000, 1, 1),
            PRODUTO_ID,
            "admin",
            StatusPortador.ATIVO,
            AGORA,
            AGORA);
    Portador segundo =
        Portador.reconstituir(
            id,
            "Outro Nome",
            CPF,
            LocalDate.of(1990, 5, 5),
            UUID.randomUUID(),
            "outro",
            StatusPortador.BLOQUEADO,
            AGORA,
            AGORA);

    assertThat(primeiro).isEqualTo(segundo);
    assertThat(primeiro).hasSameHashCodeAs(segundo);
  }

  @Test
  void deveReconstituirComTodosOsCamposPersistidos() {
    UUID id = UUID.randomUUID();
    LocalDate dataNascimento = LocalDate.of(1990, 5, 5);

    Portador portador =
        Portador.reconstituir(
            id,
            "Victor",
            CPF,
            dataNascimento,
            PRODUTO_ID,
            "admin",
            StatusPortador.BLOQUEADO,
            AGORA,
            DEPOIS);

    assertThat(portador.getId()).isEqualTo(id);
    assertThat(portador.getDataNascimento()).isEqualTo(dataNascimento);
    assertThat(portador.getCriadoPor()).isEqualTo("admin");
    assertThat(portador.getStatus()).isEqualTo(StatusPortador.BLOQUEADO);
    assertThat(portador.getAtualizadoEm()).isEqualTo(DEPOIS);
  }
}

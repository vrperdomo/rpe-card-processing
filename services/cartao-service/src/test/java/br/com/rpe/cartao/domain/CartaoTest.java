package br.com.rpe.cartao.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.com.rpe.cartao.domain.exception.RegraNegocioException;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class CartaoTest {

  private static final Instant AGORA = Instant.parse("2026-09-17T12:00:00Z");
  private static final Instant DEPOIS = Instant.parse("2026-09-18T12:00:00Z");

  private Cartao cartaoAtivo() {
    return Cartao.emitir(
        UUID.randomUUID(),
        UUID.randomUUID(),
        Pan.gerar("453201"),
        "JOAO DA SILVA",
        Validade.gerar(AGORA),
        "admin",
        AGORA);
  }

  @Test
  void deveEmitirCartaoComStatusAtivo() {
    Cartao cartao = cartaoAtivo();

    assertThat(cartao.getStatus()).isEqualTo(StatusCartao.ATIVO);
    assertThat(cartao.getCriadoEm()).isEqualTo(AGORA);
    assertThat(cartao.getAtualizadoEm()).isEqualTo(AGORA);
  }

  @Test
  void deveBloquearCartaoAtivo() {
    Cartao cartao = cartaoAtivo();

    cartao.bloquear(DEPOIS);

    assertThat(cartao.getStatus()).isEqualTo(StatusCartao.BLOQUEADO);
    assertThat(cartao.getAtualizadoEm()).isEqualTo(DEPOIS);
  }

  @Test
  void deveReativarCartaoBloqueado() {
    Cartao cartao = cartaoAtivo();
    cartao.bloquear(AGORA);

    cartao.ativar(DEPOIS);

    assertThat(cartao.getStatus()).isEqualTo(StatusCartao.ATIVO);
    assertThat(cartao.getAtualizadoEm()).isEqualTo(DEPOIS);
  }

  @Test
  void deveCancelarCartaoAtivo() {
    Cartao cartao = cartaoAtivo();

    cartao.cancelar(DEPOIS);

    assertThat(cartao.getStatus()).isEqualTo(StatusCartao.CANCELADO);
  }

  @Test
  void deveCancelarCartaoBloqueado() {
    Cartao cartao = cartaoAtivo();
    cartao.bloquear(AGORA);

    cartao.cancelar(DEPOIS);

    assertThat(cartao.getStatus()).isEqualTo(StatusCartao.CANCELADO);
  }

  @Test
  void naoDeveBloquearCartaoJaCancelado() {
    Cartao cartao = cartaoAtivo();
    cartao.cancelar(AGORA);

    assertThatThrownBy(() -> cartao.bloquear(DEPOIS))
        .isInstanceOf(RegraNegocioException.class)
        .hasMessageContaining("cancelado");
  }

  @Test
  void naoDeveBloquearCartaoJaBloqueado() {
    Cartao cartao = cartaoAtivo();
    cartao.bloquear(AGORA);

    assertThatThrownBy(() -> cartao.bloquear(DEPOIS))
        .isInstanceOf(RegraNegocioException.class)
        .hasMessageContaining("já está bloqueado");
  }

  @Test
  void naoDeveReativarCartaoCancelado() {
    Cartao cartao = cartaoAtivo();
    cartao.cancelar(AGORA);

    assertThatThrownBy(() -> cartao.ativar(DEPOIS))
        .isInstanceOf(RegraNegocioException.class)
        .hasMessageContaining("cancelado");
  }

  @Test
  void naoDeveReativarCartaoJaAtivo() {
    Cartao cartao = cartaoAtivo();

    assertThatThrownBy(() -> cartao.ativar(DEPOIS))
        .isInstanceOf(RegraNegocioException.class)
        .hasMessageContaining("já está ativo");
  }

  @Test
  void naoDeveCancelarCartaoJaCancelado() {
    Cartao cartao = cartaoAtivo();
    cartao.cancelar(AGORA);

    assertThatThrownBy(() -> cartao.cancelar(DEPOIS))
        .isInstanceOf(RegraNegocioException.class)
        .hasMessageContaining("já está cancelado");
  }

  @Test
  void deveRecusarNomeImpressoVazio() {
    assertThatThrownBy(
            () ->
                Cartao.emitir(
                    UUID.randomUUID(),
                    UUID.randomUUID(),
                    Pan.gerar("453201"),
                    " ",
                    Validade.gerar(AGORA),
                    "admin",
                    AGORA))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void deveRecusarNomeImpressoMuitoLongo() {
    String nomeMuitoLongo = "NOME COM MAIS DE VINTE E SEIS CARACTERES";

    assertThatThrownBy(
            () ->
                Cartao.emitir(
                    UUID.randomUUID(),
                    UUID.randomUUID(),
                    Pan.gerar("453201"),
                    nomeMuitoLongo,
                    Validade.gerar(AGORA),
                    "admin",
                    AGORA))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void doisCartoesComMesmoIdDevemSerIguais() {
    UUID id = UUID.randomUUID();
    Instant agora = AGORA;
    Cartao primeiro =
        Cartao.reconstituir(
            id,
            UUID.randomUUID(),
            UUID.randomUUID(),
            Pan.gerar("453201"),
            "JOAO DA SILVA",
            Validade.gerar(agora),
            "admin",
            StatusCartao.ATIVO,
            agora,
            agora);
    Cartao segundo =
        Cartao.reconstituir(
            id,
            UUID.randomUUID(),
            UUID.randomUUID(),
            Pan.gerar("453201"),
            "MARIA SOUZA",
            Validade.gerar(agora),
            "admin",
            StatusCartao.BLOQUEADO,
            agora,
            agora);

    assertThat(primeiro).isEqualTo(segundo);
  }

  @Test
  void deveGuardarQuemCadastrouComoDono() {
    assertThat(cartaoAtivo().getCriadoPor()).isEqualTo("admin");
  }

  @Test
  void deveRejeitarCriadoPorVazio() {
    for (String invalido : new String[] {null, " "}) {
      assertThatThrownBy(
              () ->
                  Cartao.emitir(
                      UUID.randomUUID(),
                      UUID.randomUUID(),
                      Pan.gerar("453201"),
                      "VICTOR",
                      Validade.gerar(AGORA),
                      invalido,
                      AGORA))
          .isInstanceOf(IllegalArgumentException.class);
    }
  }
}

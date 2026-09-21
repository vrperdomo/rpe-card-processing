package br.com.rpe.cartao.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class EmissaoFalhaTest {

  private static final Instant AGORA = Instant.parse("2026-09-21T10:00:00Z");

  @Test
  void deveCriarComOsDadosInformados() {
    UUID portadorId = UUID.randomUUID();
    UUID produtoId = UUID.randomUUID();

    EmissaoFalha falha =
        new EmissaoFalha(portadorId, produtoId, "Produto inexistente", "admin", AGORA);

    assertThat(falha.portadorId()).isEqualTo(portadorId);
    assertThat(falha.produtoId()).isEqualTo(produtoId);
    assertThat(falha.motivo()).isEqualTo("Produto inexistente");
    assertThat(falha.criadoPor()).isEqualTo("admin");
    assertThat(falha.ocorridaEm()).isEqualTo(AGORA);
  }

  @Test
  void deveTruncarMotivoLongoParaCaberNaColuna() {
    String longo = "x".repeat(EmissaoFalha.MOTIVO_TAMANHO_MAXIMO + 50);

    EmissaoFalha falha =
        new EmissaoFalha(UUID.randomUUID(), UUID.randomUUID(), longo, "admin", AGORA);

    assertThat(falha.motivo()).hasSize(EmissaoFalha.MOTIVO_TAMANHO_MAXIMO);
  }

  @Test
  void deveRejeitarMotivoOuDonoVazios() {
    UUID id = UUID.randomUUID();

    assertThatThrownBy(() -> new EmissaoFalha(id, id, " ", "admin", AGORA))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> new EmissaoFalha(id, id, null, "admin", AGORA))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> new EmissaoFalha(id, id, "motivo", " ", AGORA))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void deveRejeitarIdsEDataNulos() {
    UUID id = UUID.randomUUID();

    assertThatThrownBy(() -> new EmissaoFalha(null, id, "m", "admin", AGORA))
        .isInstanceOf(NullPointerException.class);
    assertThatThrownBy(() -> new EmissaoFalha(id, null, "m", "admin", AGORA))
        .isInstanceOf(NullPointerException.class);
    assertThatThrownBy(() -> new EmissaoFalha(id, id, "m", "admin", null))
        .isInstanceOf(NullPointerException.class);
  }
}

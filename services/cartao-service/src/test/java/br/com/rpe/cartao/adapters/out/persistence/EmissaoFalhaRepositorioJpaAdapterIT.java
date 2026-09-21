package br.com.rpe.cartao.adapters.out.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.rpe.cartao.IntegrationTestBase;
import br.com.rpe.cartao.domain.EmissaoFalha;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(EmissaoFalhaRepositorioJpaAdapter.class)
class EmissaoFalhaRepositorioJpaAdapterIT extends IntegrationTestBase {

  private static final Instant AGORA = Instant.parse("2026-09-21T10:00:00Z");

  @Autowired private EmissaoFalhaRepositorioJpaAdapter adapter;
  @Autowired private TestEntityManager entityManager;

  private void gravarELimparCache(EmissaoFalha falha) {
    adapter.registrar(falha);
    entityManager.flush();
    entityManager.clear();
  }

  @Test
  void deveGravarERecuperarAFalhaDoPortador() {
    UUID portadorId = UUID.randomUUID();
    UUID produtoId = UUID.randomUUID();
    gravarELimparCache(
        new EmissaoFalha(portadorId, produtoId, "Produto inexistente", "admin", AGORA));

    EmissaoFalha recuperada = adapter.buscarPorPortadorId(portadorId).orElseThrow();

    assertThat(recuperada.portadorId()).isEqualTo(portadorId);
    assertThat(recuperada.produtoId()).isEqualTo(produtoId);
    assertThat(recuperada.motivo()).isEqualTo("Produto inexistente");
    assertThat(recuperada.criadoPor()).isEqualTo("admin");
    assertThat(recuperada.ocorridaEm()).isEqualTo(AGORA);
  }

  @Test
  void novaFalhaDoMesmoPortadorDeveSubstituirAAnterior() {
    UUID portadorId = UUID.randomUUID();
    UUID produtoId = UUID.randomUUID();
    gravarELimparCache(new EmissaoFalha(portadorId, produtoId, "primeira", "admin", AGORA));

    gravarELimparCache(
        new EmissaoFalha(portadorId, produtoId, "segunda", "admin", AGORA.plusSeconds(60)));

    EmissaoFalha recuperada = adapter.buscarPorPortadorId(portadorId).orElseThrow();
    assertThat(recuperada.motivo()).isEqualTo("segunda");
    assertThat(recuperada.ocorridaEm()).isEqualTo(AGORA.plusSeconds(60));
  }

  @Test
  void deveRemoverAFalhaSemAfetarAsDeOutrosPortadores() {
    UUID removida = UUID.randomUUID();
    UUID mantida = UUID.randomUUID();
    gravarELimparCache(new EmissaoFalha(removida, UUID.randomUUID(), "m", "admin", AGORA));
    gravarELimparCache(new EmissaoFalha(mantida, UUID.randomUUID(), "m", "admin", AGORA));

    adapter.removerPorPortadorId(removida);
    entityManager.flush();
    entityManager.clear();

    assertThat(adapter.buscarPorPortadorId(removida)).isEmpty();
    assertThat(adapter.buscarPorPortadorId(mantida)).isPresent();
  }

  @Test
  void removerFalhaInexistenteNaoDeveFalhar() {
    adapter.removerPorPortadorId(UUID.randomUUID());
  }
}

package br.com.rpe.portador.adapters.out.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.com.rpe.portador.IntegrationTestBase;
import br.com.rpe.portador.config.ClockConfig;
import br.com.rpe.portador.config.JpaAuditingConfig;
import br.com.rpe.portador.domain.Cpf;
import br.com.rpe.portador.domain.Portador;
import br.com.rpe.portador.domain.StatusPortador;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.UUID;
import org.hibernate.exception.ConstraintViolationException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({
  ClockConfig.class,
  JpaAuditingConfig.class,
  PortadorEntityMapperImpl.class,
  PortadorRepositorioJpaAdapter.class
})
class PortadorRepositorioJpaAdapterIT extends IntegrationTestBase {

  private static final Instant AGORA = Instant.parse("2026-09-19T12:00:00Z");
  private static final Clock RELOGIO = Clock.fixed(AGORA, ZoneOffset.UTC);
  private static final UUID PRODUTO_ID = UUID.randomUUID();

  @Autowired private PortadorRepositorioJpaAdapter adapter;
  @Autowired private TestEntityManager entityManager;

  private Portador novoPortador(String cpf) {
    return Portador.cadastrar(
        "Victor Rodrigues", Cpf.of(cpf), LocalDate.of(2000, 1, 1), PRODUTO_ID, "admin", AGORA);
  }

  @Test
  void deveSalvarERecuperarPortadorPreservandoDados() {
    Portador portador = novoPortador("52998224725");

    Portador salvo = adapter.salvar(portador);
    entityManager.flush();
    entityManager.clear();

    Portador recuperado = adapter.buscarPorId(salvo.getId()).orElseThrow();
    assertThat(recuperado.getNome()).isEqualTo("Victor Rodrigues");
    assertThat(recuperado.getCpf()).isEqualTo(Cpf.of("52998224725"));
    assertThat(recuperado.getDataNascimento()).isEqualTo(LocalDate.of(2000, 1, 1));
    assertThat(recuperado.getProdutoId()).isEqualTo(PRODUTO_ID);
    assertThat(recuperado.getCriadoPor()).isEqualTo("admin");
    assertThat(recuperado.getStatus()).isEqualTo(StatusPortador.ATIVO);
  }

  @Test
  void deveAtualizarPortadorExistenteAoSalvarNovamente() {
    Portador portador = adapter.salvar(novoPortador("52998224725"));
    entityManager.flush();
    entityManager.clear();

    Portador carregado = adapter.buscarPorId(portador.getId()).orElseThrow();
    carregado.bloquear(RELOGIO.instant());
    adapter.salvar(carregado);
    entityManager.flush();
    entityManager.clear();

    Portador recarregado = adapter.buscarPorId(portador.getId()).orElseThrow();
    assertThat(recarregado.getStatus()).isEqualTo(StatusPortador.BLOQUEADO);
    assertThat(recarregado.getCriadoPor()).isEqualTo("admin");
  }

  @Test
  void devePropagarExistenciaPorCpf() {
    adapter.salvar(novoPortador("52998224725"));

    assertThat(adapter.existePorCpf("52998224725")).isTrue();
    assertThat(adapter.existePorCpf("11144477735")).isFalse();
  }

  @Test
  void deveGarantirUnicidadeDeCpfNoBanco() {
    adapter.salvar(novoPortador("52998224725"));
    entityManager.flush();

    // A violação só ocorre no flush manual (fora do proxy do Spring Data), por isso chega como
    // exceção crua do Hibernate, não traduzida para DataIntegrityViolationException — a tradução
    // acontece na fronteira do proxy do repositório, testada separadamente no
    // GlobalExceptionHandler.
    assertThatThrownBy(
            () -> {
              adapter.salvar(novoPortador("52998224725"));
              entityManager.flush();
            })
        .isInstanceOf(ConstraintViolationException.class);
  }
}

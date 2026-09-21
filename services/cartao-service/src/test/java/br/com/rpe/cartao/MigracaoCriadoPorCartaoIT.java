package br.com.rpe.cartao;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.UUID;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;

/**
 * A migração V3 (ADR-009, A01) roda sobre dados já existentes: linhas anteriores recebem o dono
 * 'legado' (que nenhum usuário possui) e, depois do backfill, o DEFAULT some, de modo que todo
 * INSERT novo tem de informar o dono. Roda num schema isolado para migrar em duas etapas (V2 e V3)
 * no mesmo Postgres real dos demais testes.
 */
class MigracaoCriadoPorCartaoIT extends IntegrationTestBase {

  private static final String SCHEMA = "migracao_criado_por_cartao";

  private Flyway flyway(String alvo) {
    return Flyway.configure()
        .dataSource(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword())
        .schemas(SCHEMA)
        .locations("classpath:db/migration")
        .target(alvo)
        .load();
  }

  private Connection conexao() throws SQLException {
    Connection conexao =
        DriverManager.getConnection(
            POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword());
    try (Statement st = conexao.createStatement()) {
      st.execute("SET search_path TO " + SCHEMA);
    }
    return conexao;
  }

  private static String inserirSemDono(UUID id) {
    return """
        INSERT INTO cartao (id, portador_id, produto_id, pan_cifrado, pan_hash, ultimos4,
                            nome_impresso, validade, status, criado_em, atualizado_em)
        VALUES ('%s', '%s', '%s', 'cifrado', '%s', '0366', 'ANTIGO', '09/31', 'ATIVO', now(), now())
        """
        .formatted(id, UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID());
  }

  @Test
  void deveMarcarLinhasAnterioresComoLegadoEExigirDonoNosNovosInserts() throws Exception {
    flyway("2").migrate();
    UUID antigo = UUID.randomUUID();
    try (Connection c = conexao();
        Statement st = c.createStatement()) {
      st.execute(inserirSemDono(antigo));
    }

    flyway("3").migrate();

    try (Connection c = conexao();
        Statement st = c.createStatement()) {
      var rs = st.executeQuery("SELECT criado_por FROM cartao WHERE id = '%s'".formatted(antigo));
      assertThat(rs.next()).isTrue();
      assertThat(rs.getString(1)).isEqualTo("legado");
      assertThatThrownBy(() -> st.execute(inserirSemDono(UUID.randomUUID())))
          .isInstanceOf(SQLException.class)
          .hasMessageContaining("criado_por");
    }
  }
}

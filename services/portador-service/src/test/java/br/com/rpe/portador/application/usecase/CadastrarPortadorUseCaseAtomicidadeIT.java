package br.com.rpe.portador.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;

import br.com.rpe.portador.IntegrationTestBase;
import br.com.rpe.portador.adapters.out.persistence.PortadorEntityMapperImpl;
import br.com.rpe.portador.adapters.out.persistence.PortadorJpaRepository;
import br.com.rpe.portador.adapters.out.persistence.PortadorRepositorioJpaAdapter;
import br.com.rpe.portador.application.port.out.OutboxRepositorio;
import br.com.rpe.portador.application.port.out.ProdutoClient;
import br.com.rpe.portador.application.port.out.ProdutoDto;
import br.com.rpe.portador.application.port.out.StatusProdutoExterno;
import br.com.rpe.portador.config.ClockConfig;
import br.com.rpe.portador.config.JpaAuditingConfig;
import br.com.rpe.portador.domain.Cpf;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.transaction.TestTransaction;

// Prova PO-06: portador e evento de outbox sao gravados na MESMA transacao. Se a escrita do
// outbox falhar, o portador tambem nao pode ficar persistido (rollback), mesmo que o INSERT do
// portador em si tenha tido sucesso antes da falha.
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({
  ClockConfig.class,
  JpaAuditingConfig.class,
  PortadorEntityMapperImpl.class,
  PortadorRepositorioJpaAdapter.class,
  CadastrarPortadorUseCase.class
})
class CadastrarPortadorUseCaseAtomicidadeIT extends IntegrationTestBase {

  private static final UUID PRODUTO_ID = UUID.randomUUID();
  private static final Cpf CPF = Cpf.of("52998224725");

  @Autowired private CadastrarPortadorUseCase useCase;
  @Autowired private PortadorJpaRepository portadorJpaRepository;

  @MockitoBean private ProdutoClient produtoClient;
  @MockitoBean private OutboxRepositorio outboxRepositorio;

  @Test
  void naoDevePersistirPortadorQuandoEscritaDoOutboxFalha() {
    when(produtoClient.buscarPorId(PRODUTO_ID))
        .thenReturn(Optional.of(new ProdutoDto(PRODUTO_ID, StatusProdutoExterno.ATIVO)));
    doThrow(new IllegalStateException("falha simulada ao gravar outbox"))
        .when(outboxRepositorio)
        .registrar(any(), any(), any());

    assertThatThrownBy(
            () ->
                useCase.executar(
                    "Victor", CPF, LocalDate.of(2000, 1, 1), PRODUTO_ID, "corr-atomicidade"))
        .isInstanceOf(IllegalStateException.class);

    // A exceção só marca a transação (compartilhada com o teste) como rollback-only; o rollback
    // físico só acontece quando a transação termina. Encerramos e abrimos uma nova para consultar
    // um estado que já refletiu o rollback, e não a escrita ainda pendente na mesma transação.
    TestTransaction.end();
    TestTransaction.start();
    assertThat(portadorJpaRepository.existsByCpf(CPF.valor())).isFalse();
  }
}

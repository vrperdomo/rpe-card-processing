package br.com.rpe.cartao.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import br.com.rpe.cartao.application.port.out.EmissaoFalhaRepositorio;
import br.com.rpe.cartao.application.seguranca.Solicitante;
import br.com.rpe.cartao.domain.EmissaoFalha;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class RegistrarFalhaEmissaoUseCaseTest {

  private static final Instant AGORA = Instant.parse("2026-09-21T10:00:00Z");

  private final EmissaoFalhaRepositorio repositorio = mock(EmissaoFalhaRepositorio.class);
  private final SimpleMeterRegistry meterRegistry = new SimpleMeterRegistry();
  private final RegistrarFalhaEmissaoUseCase useCase =
      new RegistrarFalhaEmissaoUseCase(
          repositorio, meterRegistry, Clock.fixed(AGORA, ZoneOffset.UTC));

  private EmissaoFalha registrada() {
    ArgumentCaptor<EmissaoFalha> captor = ArgumentCaptor.forClass(EmissaoFalha.class);
    verify(repositorio).registrar(captor.capture());
    return captor.getValue();
  }

  @Test
  void deveRegistrarFalhaDefinitivaComDonoMotivoEHorario() {
    UUID portadorId = UUID.randomUUID();
    UUID produtoId = UUID.randomUUID();

    useCase.registrarDefinitiva(portadorId, produtoId, "admin", "Produto inexistente ou não ATIVO");

    EmissaoFalha falha = registrada();
    assertThat(falha.portadorId()).isEqualTo(portadorId);
    assertThat(falha.produtoId()).isEqualTo(produtoId);
    assertThat(falha.criadoPor()).isEqualTo("admin");
    assertThat(falha.motivo()).isEqualTo("Produto inexistente ou não ATIVO");
    assertThat(falha.ocorridaEm()).isEqualTo(AGORA);
    assertThat(meterRegistry.counter("cartao.emissao.falhas", "tipo", "definitiva").count())
        .isEqualTo(1.0);
  }

  @Test
  void deveRegistrarTentativasEsgotadasComMotivoFixoESemDetalheInterno() {
    useCase.registrarTentativasEsgotadas(UUID.randomUUID(), UUID.randomUUID(), "admin");

    EmissaoFalha falha = registrada();
    assertThat(falha.motivo())
        .isEqualTo("Tentativas de emissão esgotadas: dependência indisponível");
    assertThat(
            meterRegistry.counter("cartao.emissao.falhas", "tipo", "tentativas_esgotadas").count())
        .isEqualTo(1.0);
  }

  @Test
  void deveAtribuirDonoLegadoQuandoEventoNaoTrazCriadoPor() {
    useCase.registrarDefinitiva(UUID.randomUUID(), UUID.randomUUID(), null, "motivo");

    assertThat(registrada().criadoPor()).isEqualTo(Solicitante.DONO_LEGADO);
  }
}

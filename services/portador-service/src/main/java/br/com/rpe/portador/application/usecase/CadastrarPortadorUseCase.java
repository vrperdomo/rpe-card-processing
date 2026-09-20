package br.com.rpe.portador.application.usecase;

import br.com.rpe.portador.application.evento.CartaoEmissaoSolicitadaData;
import br.com.rpe.portador.application.evento.EventoOutbox;
import br.com.rpe.portador.application.port.out.OutboxRepositorio;
import br.com.rpe.portador.application.port.out.PortadorRepositorio;
import br.com.rpe.portador.application.port.out.ProdutoClient;
import br.com.rpe.portador.application.port.out.ProdutoDto;
import br.com.rpe.portador.application.port.out.StatusProdutoExterno;
import br.com.rpe.portador.domain.Cpf;
import br.com.rpe.portador.domain.Portador;
import br.com.rpe.portador.domain.exception.ConflitoException;
import br.com.rpe.portador.domain.exception.RegraNegocioException;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CadastrarPortadorUseCase {

  private static final String AGGREGATE_TYPE = "Portador";
  private static final String EVENT_TYPE = "CartaoEmissaoSolicitada";
  private static final int NOME_IMPRESSO_TAMANHO_MAXIMO = 26;

  private final PortadorRepositorio portadorRepositorio;
  private final ProdutoClient produtoClient;
  private final OutboxRepositorio outboxRepositorio;
  private final Clock clock;

  public CadastrarPortadorUseCase(
      PortadorRepositorio portadorRepositorio,
      ProdutoClient produtoClient,
      OutboxRepositorio outboxRepositorio,
      Clock clock) {
    this.portadorRepositorio = portadorRepositorio;
    this.produtoClient = produtoClient;
    this.outboxRepositorio = outboxRepositorio;
    this.clock = clock;
  }

  @Transactional
  public Portador executar(
      String nome, Cpf cpf, LocalDate dataNascimento, UUID produtoId, String correlationId) {
    validarProdutoAtivo(produtoId);
    if (portadorRepositorio.existePorCpf(cpf.valor())) {
      throw new ConflitoException("Já existe um portador cadastrado com este CPF");
    }
    Instant agora = Instant.now(clock);
    Portador portador = Portador.cadastrar(nome, cpf, dataNascimento, produtoId, agora);
    Portador salvo = portadorRepositorio.salvar(portador);
    outboxRepositorio.registrar(
        salvo.getId(), AGGREGATE_TYPE, criarEventoEmissao(salvo, correlationId, agora));
    return salvo;
  }

  private EventoOutbox criarEventoEmissao(Portador portador, String correlationId, Instant agora) {
    CartaoEmissaoSolicitadaData data =
        new CartaoEmissaoSolicitadaData(
            portador.getId(), portador.getProdutoId(), nomeImpresso(portador.getNome()));
    return EventoOutbox.criar(EVENT_TYPE, correlationId, data, agora);
  }

  private String nomeImpresso(String nome) {
    String maiusculo = nome.toUpperCase();
    return maiusculo.length() > NOME_IMPRESSO_TAMANHO_MAXIMO
        ? maiusculo.substring(0, NOME_IMPRESSO_TAMANHO_MAXIMO)
        : maiusculo;
  }

  private void validarProdutoAtivo(UUID produtoId) {
    ProdutoDto produto =
        produtoClient
            .buscarPorId(produtoId)
            .orElseThrow(
                () -> new RegraNegocioException("Produto %s não encontrado".formatted(produtoId)));
    if (produto.status() != StatusProdutoExterno.ATIVO) {
      throw new RegraNegocioException("Produto %s não está ATIVO".formatted(produtoId));
    }
  }
}

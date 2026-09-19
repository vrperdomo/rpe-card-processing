package br.com.rpe.portador.application.usecase;

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

  private final PortadorRepositorio portadorRepositorio;
  private final ProdutoClient produtoClient;
  private final Clock clock;

  public CadastrarPortadorUseCase(
      PortadorRepositorio portadorRepositorio, ProdutoClient produtoClient, Clock clock) {
    this.portadorRepositorio = portadorRepositorio;
    this.produtoClient = produtoClient;
    this.clock = clock;
  }

  @Transactional
  public Portador executar(String nome, Cpf cpf, LocalDate dataNascimento, UUID produtoId) {
    validarProdutoAtivo(produtoId);
    if (portadorRepositorio.existePorCpf(cpf.valor())) {
      throw new ConflitoException("Já existe um portador cadastrado com este CPF");
    }
    Portador portador =
        Portador.cadastrar(nome, cpf, dataNascimento, produtoId, Instant.now(clock));
    return portadorRepositorio.salvar(portador);
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

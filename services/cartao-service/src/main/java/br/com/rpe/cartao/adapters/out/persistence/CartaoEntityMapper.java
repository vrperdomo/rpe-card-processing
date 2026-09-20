package br.com.rpe.cartao.adapters.out.persistence;

import br.com.rpe.cartao.application.port.out.CriptografoPan;
import br.com.rpe.cartao.domain.Cartao;
import br.com.rpe.cartao.domain.Pan;
import br.com.rpe.cartao.domain.Validade;
import org.springframework.stereotype.Component;

// Nao usa MapStruct de proposito: a cifra/decifra do PAN precisa do CriptografoPan injetado no
// mapeamento, o que foge do estilo puramente declarativo dos outros mappers do monorepo.
@Component
public class CartaoEntityMapper {

  private final CriptografoPan criptografoPan;

  public CartaoEntityMapper(CriptografoPan criptografoPan) {
    this.criptografoPan = criptografoPan;
  }

  public CartaoEntity paraNovaEntidade(Cartao cartao) {
    Pan pan = cartao.getPan();
    return new CartaoEntity(
        cartao.getId(),
        cartao.getPortadorId(),
        cartao.getProdutoId(),
        criptografoPan.cifrar(pan.valor()),
        criptografoPan.hash(pan.valor()),
        pan.ultimos4(),
        cartao.getNomeImpresso(),
        cartao.getValidade().valor(),
        cartao.getStatus());
  }

  public void copiarParaEntidadeExistente(Cartao origem, CartaoEntity destino) {
    destino.setStatus(origem.getStatus());
  }

  public Cartao paraDominio(CartaoEntity entity) {
    String panEmClaro = criptografoPan.decifrar(entity.getPanCifrado());
    return Cartao.reconstituir(
        entity.getId(),
        entity.getPortadorId(),
        entity.getProdutoId(),
        Pan.of(panEmClaro),
        entity.getNomeImpresso(),
        Validade.of(entity.getValidade()),
        entity.getStatus(),
        entity.getCriadoEm(),
        entity.getAtualizadoEm());
  }
}

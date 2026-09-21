package br.com.rpe.cartao.application.seguranca;

/**
 * Quem está fazendo a chamada, já extraído do JWT validado pela borda web (ADR-009, A01). A camada
 * de aplicação só conhece este tipo: nenhuma classe do Spring Security entra aqui.
 *
 * <p>Um <b>usuário</b> só acessa o que criou. Um <b>serviço</b> (token de serviço-para-serviço,
 * ex.: o Portador consultando o Cartão no {@code /completo}) acessa qualquer recurso, pois quem o
 * chama já foi autorizado pelo serviço de origem.
 */
public record Solicitante(String id, boolean servico) {

  /** Escopo (claim {@code scope}) que marca um token como de serviço-para-serviço. */
  public static final String ESCOPO_SERVICO = "servico";

  /** Dono atribuído às linhas anteriores à migração de posse; nenhum usuário o possui. */
  public static final String DONO_LEGADO = "legado";

  public Solicitante {
    if (id == null || id.isBlank()) {
      throw new IllegalArgumentException("id do solicitante não pode ser vazio");
    }
  }

  public static Solicitante deUsuario(String id) {
    return new Solicitante(id, false);
  }

  public static Solicitante deServico(String id) {
    return new Solicitante(id, true);
  }

  /** Dono a gravar para um evento: os anteriores ao #121 não trazem criadoPor e ficam 'legado'. */
  public static String donoOuLegado(String criadoPor) {
    return criadoPor == null || criadoPor.isBlank() ? DONO_LEGADO : criadoPor;
  }

  public boolean podeAcessar(String donoDoRecurso) {
    if (servico) {
      return true;
    }
    return !DONO_LEGADO.equals(donoDoRecurso) && id.equals(donoDoRecurso);
  }
}

import { ApiError } from './apiError'

// Traduz qualquer erro em texto para o usuário. Regra do CLAUDE.md 6.3 espelhada no front: nada de
// mensagem interna ou stack trace na tela; erros 5xx viram texto genérico, e o correlationId vai à
// parte (codigoDeSuporte) para o usuário informar ao suporte.
export function mensagemDeErro(erro: unknown): string {
  if (!(erro instanceof ApiError)) {
    return 'Ocorreu um erro inesperado. Tente novamente.'
  }
  if (erro.semConexao) {
    return 'Não foi possível conectar ao servidor. Verifique sua conexão e tente novamente.'
  }
  if (erro.status === 429) return mensagemDeLimite(erro.retryAfterSegundos)
  if (erro.status === 503) {
    return 'Serviço temporariamente indisponível. Tente novamente em instantes.'
  }
  if (erro.status >= 500) return 'Erro inesperado no servidor. Tente novamente em instantes.'
  return erro.detalhe ?? erro.titulo ?? 'Não foi possível concluir a operação.'
}

function mensagemDeLimite(segundos: number | undefined): string {
  if (segundos === undefined) return 'Muitas tentativas. Aguarde um pouco e tente novamente.'
  const unidade = segundos === 1 ? 'segundo' : 'segundos'
  return `Muitas tentativas. Tente novamente em ${segundos} ${unidade}.`
}

export function codigoDeSuporte(erro: unknown): string | undefined {
  return erro instanceof ApiError ? erro.correlationId : undefined
}

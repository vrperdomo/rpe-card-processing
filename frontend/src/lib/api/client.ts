import { authStore } from '../auth/authStore'
import { ApiError, lerErrosDeCampo, lerRetryAfter, type ProblemDetailBody } from './apiError'
import { novoCorrelationId } from './correlationId'

export interface OpcoesRequisicao {
  metodo?: 'GET' | 'POST' | 'PATCH' | 'PUT' | 'DELETE'
  corpo?: unknown
  // Rotas públicas (login) não enviam o token, e um 401 delas é "credenciais inválidas", não
  // "sessão expirada".
  publica?: boolean
  signal?: AbortSignal
}

// Sempre caminhos relativos (/api/v1/...): em produção o Nginx e em dev o Vite encaminham cada
// prefixo ao serviço dono (ADR-008), então o browser só conhece uma origem.
export async function apiFetch<T>(caminho: string, opcoes: OpcoesRequisicao = {}): Promise<T> {
  const { metodo = 'GET', corpo, publica = false, signal } = opcoes
  const token = publica ? null : authStore.obterToken()

  const cabecalhos: Record<string, string> = {
    Accept: 'application/json',
    // O backend propaga esse id em logs e eventos (CLAUDE.md 6.8) e o devolve nos erros.
    'X-Correlation-Id': novoCorrelationId(),
  }
  if (corpo !== undefined) cabecalhos['Content-Type'] = 'application/json'
  if (token !== null) cabecalhos['Authorization'] = `Bearer ${token}`

  let resposta: Response
  try {
    resposta = await fetch(caminho, {
      method: metodo,
      headers: cabecalhos,
      body: corpo === undefined ? null : JSON.stringify(corpo),
      signal: signal ?? null,
    })
  } catch (causa) {
    if (causa instanceof DOMException && causa.name === 'AbortError') throw causa
    throw new ApiError({ status: 0 })
  }

  if (!resposta.ok) {
    if (resposta.status === 401 && token !== null) authStore.encerrar('nao-autorizado')
    throw await erroDaResposta(resposta)
  }
  if (resposta.status === 204) return undefined as T
  return (await resposta.json()) as T
}

async function erroDaResposta(resposta: Response): Promise<ApiError> {
  const problema = await lerProblema(resposta)
  return new ApiError({
    status: resposta.status,
    titulo: problema.title,
    detalhe: problema.detail,
    correlationId: problema.correlationId,
    retryAfterSegundos: lerRetryAfter(resposta.headers.get('Retry-After')),
    errosDeCampo: lerErrosDeCampo(problema.errors),
  })
}

// Só confia no corpo se for JSON; qualquer outra coisa (HTML de proxy, texto) vira erro genérico
// e nunca é exibida ao usuário.
async function lerProblema(resposta: Response): Promise<ProblemDetailBody> {
  const tipo = resposta.headers.get('Content-Type') ?? ''
  if (!tipo.includes('json')) return {}
  try {
    const corpo: unknown = await resposta.json()
    return typeof corpo === 'object' && corpo !== null ? (corpo as ProblemDetailBody) : {}
  } catch {
    return {}
  }
}

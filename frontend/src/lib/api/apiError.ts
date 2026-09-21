// Corpo de erro do backend: ProblemDetail (RFC 9457) com correlationId (CLAUDE.md 6.3).
export interface ProblemDetailBody {
  type?: string
  title?: string
  status?: number
  detail?: string
  correlationId?: string
}

// status 0 = a requisição nem chegou a ter resposta (rede fora, DNS, CORS, abortada).
export class ApiError extends Error {
  readonly status: number
  readonly titulo: string | undefined
  readonly detalhe: string | undefined
  // Identificador para o suporte achar a requisição nos logs dos serviços.
  readonly correlationId: string | undefined
  readonly retryAfterSegundos: number | undefined

  constructor(init: {
    status: number
    titulo?: string
    detalhe?: string
    correlationId?: string
    retryAfterSegundos?: number
  }) {
    super(init.detalhe ?? init.titulo ?? `Erro HTTP ${init.status}`)
    this.name = 'ApiError'
    this.status = init.status
    this.titulo = init.titulo
    this.detalhe = init.detalhe
    this.correlationId = init.correlationId
    this.retryAfterSegundos = init.retryAfterSegundos
  }

  get semConexao(): boolean {
    return this.status === 0
  }
}

// Retry-After pode vir em segundos ou como data HTTP; só o formato em segundos é tratado.
export function lerRetryAfter(valor: string | null): number | undefined {
  if (valor === null) return undefined
  const segundos = Number.parseInt(valor, 10)
  return Number.isFinite(segundos) && segundos >= 0 ? segundos : undefined
}

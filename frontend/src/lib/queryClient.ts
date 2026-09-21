import { QueryClient } from '@tanstack/react-query'
import { ApiError } from './api/apiError'

// Erros 4xx são respostas definitivas do backend (validação, regra de negócio, 401): repetir a
// mesma requisição não muda nada. Só falha de rede e 5xx merecem novas tentativas.
export function deveTentarNovamente(tentativas: number, erro: unknown): boolean {
  const definitivo = erro instanceof ApiError && erro.status >= 400 && erro.status < 500
  return !definitivo && tentativas < 2
}

export function criarQueryClient(): QueryClient {
  return new QueryClient({
    defaultOptions: {
      queries: { retry: deveTentarNovamente, refetchOnWindowFocus: false },
      // Mutations (POST/PATCH) não são idempotentes por padrão: nunca repetir sozinhas.
      mutations: { retry: false },
    },
  })
}

import { QueryClientProvider } from '@tanstack/react-query'
import { render } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter } from 'react-router-dom'
import { vi } from 'vitest'
import App from '../App'
import { criarQueryClient } from '../lib/queryClient'

export function renderApp(rota = '/') {
  const queryClient = criarQueryClient()
  const user = userEvent.setup()
  const resultado = render(
    <QueryClientProvider client={queryClient}>
      <MemoryRouter initialEntries={[rota]}>
        <App />
      </MemoryRouter>
    </QueryClientProvider>,
  )
  return { user, queryClient, ...resultado }
}

// Resposta no formato do backend: erros vêm como application/problem+json (RFC 9457).
export function respostaJson(status: number, corpo?: unknown, cabecalhos: Record<string, string> = {}) {
  const tipo = status >= 400 ? 'application/problem+json' : 'application/json'
  return new Response(corpo === undefined ? null : JSON.stringify(corpo), {
    status,
    headers: { 'Content-Type': tipo, ...cabecalhos },
  })
}

export function espiarFetch() {
  return vi.spyOn(globalThis, 'fetch')
}

export const RESPOSTA_LOGIN_OK = { accessToken: 'jwt-de-teste', tokenType: 'Bearer', expiresIn: 1800 }

// Cabeçalhos da N-ésima chamada ao fetch espiado (o cliente sempre passa um objeto simples).
export function cabecalhosDaChamada(fetchSpy: ReturnType<typeof espiarFetch>, indice: number) {
  const init = fetchSpy.mock.calls[indice]?.[1]
  return (init?.headers ?? {}) as Record<string, string>
}

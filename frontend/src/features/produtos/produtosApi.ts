import { useQuery } from '@tanstack/react-query'
import { z } from 'zod'
import { apiFetch } from '../../lib/api/client'

const produtoSchema = z.object({
  id: z.guid(),
  nome: z.string(),
  categoria: z.string(),
  bin: z.string(),
})

// A API pagina em português: {conteudo, pagina, tamanho, totalElementos, totalPaginas}.
const paginaDeProdutosSchema = z.object({
  conteudo: z.array(produtoSchema),
  totalElementos: z.number(),
})

export type Produto = z.infer<typeof produtoSchema>
export type PaginaDeProdutos = z.infer<typeof paginaDeProdutosSchema>

// Tamanho da página do seletor: cobre o catálogo do desafio; se houver mais, a tela avisa.
export const TAMANHO_SELETOR = 100

export async function listarProdutosAtivos(signal?: AbortSignal): Promise<PaginaDeProdutos> {
  const resposta = await apiFetch<unknown>(
    `/api/v1/produtos?status=ATIVO&page=0&size=${TAMANHO_SELETOR}`,
    signal ? { signal } : {},
  )
  return paginaDeProdutosSchema.parse(resposta)
}

export function useProdutosAtivos() {
  return useQuery({
    queryKey: ['produtos', 'ativos'],
    queryFn: ({ signal }) => listarProdutosAtivos(signal),
    // O catálogo muda pouco: evita refazer a chamada a cada abertura do formulário.
    staleTime: 60_000,
  })
}

import { vi } from 'vitest'
import { respostaJson } from './utils'

export const ID_PORTADOR = '3f2b8c1e-6d4a-4b7e-9c21-0a5d7e8f9b10'
export const ID_PRODUTO = '9b0d5b56-da49-40b1-ab1b-c4fb9fe13029'
export const ID_CARTAO = '52c359e6-6498-453a-a99a-5f52203266cc'

// CPFs com dígitos verificadores corretos (usados em documentação pública).
export const CPF_VALIDO = '529.982.247-25'
export const CPF_VALIDO_SO_DIGITOS = '52998224725'

export function paginaDeProdutos(nomes: string[] = ['Gold Teste']) {
  const conteudo = nomes.map((nome, i) => ({
    id: i === 0 ? ID_PRODUTO : `00000000-0000-4000-8000-00000000000${i}`,
    nome,
    descricao: 'Produto de teste',
    categoria: 'GOLD',
    bin: '453201',
    status: 'ATIVO',
    criadoEm: '2026-09-21T03:52:24.096001Z',
    atualizadoEm: '2026-09-21T03:52:24.096001Z',
  }))
  return { conteudo, pagina: 0, tamanho: 100, totalElementos: conteudo.length, totalPaginas: 1 }
}

export function portadorResponse(sobrescrever: Record<string, unknown> = {}) {
  return {
    id: ID_PORTADOR,
    nome: 'Victor Rodrigues',
    cpf: '***.982.247-**',
    dataNascimento: '1990-01-31',
    produtoId: ID_PRODUTO,
    status: 'ATIVO',
    criadoEm: '2026-09-21T03:52:24Z',
    atualizadoEm: '2026-09-21T03:52:24Z',
    ...sobrescrever,
  }
}

export function portadorCompleto(sobrescrever: Record<string, unknown> = {}) {
  return {
    portador: portadorResponse(),
    cartao: null,
    produto: { id: ID_PRODUTO, nome: 'Gold Teste', categoria: 'GOLD', status: 'ATIVO' },
    emissao: 'PENDENTE',
    avisos: [],
    ...sobrescrever,
  }
}

export const CARTAO_EMITIDO = {
  id: ID_CARTAO,
  panMascarado: '**** **** **** 6707',
  validade: '09/31',
  status: 'ATIVO',
}

export interface RotaFalsa {
  metodo?: string
  // Prefixo do caminho (com query, se importar).
  caminho: string
  resposta: () => Response | Promise<Response>
}

// fetch falso por rota: cada chamada monta uma Response nova (o corpo só pode ser lido uma vez) e
// qualquer chamada não prevista falha o teste em vez de passar em silêncio.
export function rotearFetch(rotas: RotaFalsa[]) {
  return vi.spyOn(globalThis, 'fetch').mockImplementation(async (entrada, init) => {
    const url = String(entrada)
    const metodo = init?.method ?? 'GET'
    const rota = rotas.find((r) => (r.metodo ?? 'GET') === metodo && url.startsWith(r.caminho))
    if (!rota) throw new Error(`fetch não previsto no teste: ${metodo} ${url}`)
    return rota.resposta()
  })
}

export const rotaProdutos = (nomes?: string[]): RotaFalsa => ({
  caminho: '/api/v1/produtos?status=ATIVO',
  resposta: () => respostaJson(200, paginaDeProdutos(nomes)),
})

export const rotaCompleto = (corpo: () => unknown, status = 200): RotaFalsa => ({
  caminho: `/api/v1/portadores/${ID_PORTADOR}/completo`,
  resposta: () => respostaJson(status, corpo()),
})

// Quantas vezes o fetch foi chamado para um caminho.
export function chamadasPara(spy: ReturnType<typeof rotearFetch>, prefixo: string, metodo = 'GET') {
  return spy.mock.calls.filter(
    ([url, init]) => String(url).startsWith(prefixo) && (init?.method ?? 'GET') === metodo,
  )
}

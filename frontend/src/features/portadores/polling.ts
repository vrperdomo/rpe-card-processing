import type { StatusEmissao } from './portadoresApi'

// A emissão do cartão é assíncrona (outbox -> SQS -> Cartão), então a tela consulta até concluir.
// FALHOU (a mensagem foi para a DLQ) é um estado final: não há o que esperar, então não há polling.
// PENDENTE, porém, pode durar mais que o esperado (fila lenta, Produto fora): por isso o polling
// tem teto, e depois dele a tela para de insistir e explica.
export const POLLING = {
  pendenteMs: 2_000,
  // Cartão fora do ar: o backend responde DESCONHECIDA + aviso. Insistir menos, sem martelar.
  desconhecidaMs: 5_000,
  limiteMs: 120_000,
} as const

export function intervaloDePolling(emissao: StatusEmissao | undefined): number | false {
  if (emissao === 'PENDENTE') return POLLING.pendenteMs
  if (emissao === 'DESCONHECIDA') return POLLING.desconhecidaMs
  return false
}

// Estados em que ainda vale esperar: só neles o teto do polling faz sentido. CONCLUIDA e FALHOU
// são finais e não têm o que "demorar".
export function emissaoEmAndamento(emissao: StatusEmissao | undefined): boolean {
  return emissao === 'PENDENTE' || emissao === 'DESCONHECIDA'
}

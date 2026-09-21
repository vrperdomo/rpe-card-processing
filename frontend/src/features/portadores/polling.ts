import type { StatusEmissao } from './portadoresApi'

// A emissão do cartão é assíncrona (outbox -> SQS -> Cartão), então a tela consulta até concluir.
// O backend não tem estado de "falha": se a mensagem for para a DLQ o portador fica PENDENTE para
// sempre. Por isso o polling tem teto: depois dele a tela para de insistir e explica.
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

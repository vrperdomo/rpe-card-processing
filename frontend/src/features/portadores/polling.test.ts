import { describe, expect, it } from 'vitest'
import { intervaloDePolling, POLLING } from './polling'

describe('intervaloDePolling', () => {
  it('deveConsultarRapidoEnquantoAEmissaoEstaPendente', () => {
    expect(intervaloDePolling('PENDENTE')).toBe(POLLING.pendenteMs)
  })

  it('deveConsultarMaisDevagarQuandoOServicoDeCartoesNaoResponde', () => {
    expect(intervaloDePolling('DESCONHECIDA')).toBe(POLLING.desconhecidaMs)
    expect(POLLING.desconhecidaMs).toBeGreaterThan(POLLING.pendenteMs)
  })

  it('deveParaDeConsultarQuandoAEmissaoConcluiOuAindaNaoHaDados', () => {
    expect(intervaloDePolling('CONCLUIDA')).toBe(false)
    expect(intervaloDePolling(undefined)).toBe(false)
  })
})

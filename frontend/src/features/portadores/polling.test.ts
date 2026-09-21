import { describe, expect, it } from 'vitest'
import { emissaoEmAndamento, intervaloDePolling, POLLING } from './polling'

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

  it('deveParaDeConsultarQuandoAEmissaoFalhouPoisOEstadoEFinal', () => {
    expect(intervaloDePolling('FALHOU')).toBe(false)
  })
})

describe('emissaoEmAndamento', () => {
  it('deveConsiderarEmAndamentoSoOsEstadosEmQueAindaValeEsperar', () => {
    expect(emissaoEmAndamento('PENDENTE')).toBe(true)
    expect(emissaoEmAndamento('DESCONHECIDA')).toBe(true)
  })

  it('deveConsiderarFinaisACONCLUIDAeAFALHOU', () => {
    expect(emissaoEmAndamento('CONCLUIDA')).toBe(false)
    expect(emissaoEmAndamento('FALHOU')).toBe(false)
    expect(emissaoEmAndamento(undefined)).toBe(false)
  })
})

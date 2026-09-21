import { describe, expect, it } from 'vitest'
import { formatarInstante } from './dataHora'

describe('formatarInstante', () => {
  it('deveFormatarDataEHoraEmPortuguesNoFusoInformado', () => {
    expect(formatarInstante('2026-09-21T15:30:00Z', 'UTC')).toMatch(/^21\/09\/2026\D+15:30$/)
  })

  it('deveConverterParaOFusoDeQuemOlha', () => {
    expect(formatarInstante('2026-09-21T02:30:00Z', 'America/Sao_Paulo')).toMatch(/^20\/09\/2026\D+23:30$/)
  })

  it('deveDevolverOTextoRecebidoQuandoNaoForUmaData', () => {
    expect(formatarInstante('ontem')).toBe('ontem')
  })
})

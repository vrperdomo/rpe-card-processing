import { describe, expect, it } from 'vitest'
import { estaNoPassado, formatarDataIso, hojeCivil, idadeEmAnos, lerDataIso } from './data'

const HOJE = { ano: 2026, mes: 9, dia: 21 }

describe('lerDataIso', () => {
  it('deveLerDataValida', () => {
    expect(lerDataIso('1990-01-31')).toEqual({ ano: 1990, mes: 1, dia: 31 })
  })

  it('deveRejeitarFormatoInvalido', () => {
    for (const valor of ['', '31/01/1990', '1990-1-31', 'abc', '1990-01-31T00:00:00']) {
      expect(lerDataIso(valor)).toBeNull()
    }
  })

  it('deveRejeitarDataQueNaoExiste', () => {
    expect(lerDataIso('2026-02-31')).toBeNull()
    expect(lerDataIso('2026-13-01')).toBeNull()
    expect(lerDataIso('2025-02-29')).toBeNull()
  })

  it('deveAceitar29DeFevereiroEmAnoBissexto', () => {
    expect(lerDataIso('2024-02-29')).toEqual({ ano: 2024, mes: 2, dia: 29 })
  })
})

describe('idadeEmAnos', () => {
  it('deveContar18AnosNoDiaDoAniversario', () => {
    expect(idadeEmAnos({ ano: 2008, mes: 9, dia: 21 }, HOJE)).toBe(18)
  })

  it('deveContar17AnosUmDiaAntesDoAniversario', () => {
    expect(idadeEmAnos({ ano: 2008, mes: 9, dia: 22 }, HOJE)).toBe(17)
  })

  it('deveContarCorretamenteQuandoOMesDoAniversarioAindaNaoChegou', () => {
    expect(idadeEmAnos({ ano: 2000, mes: 12, dia: 1 }, HOJE)).toBe(25)
  })
})

describe('estaNoPassado', () => {
  it('deveConsiderarSomenteDatasEstritamenteAnteriores', () => {
    expect(estaNoPassado({ ano: 2026, mes: 9, dia: 20 }, HOJE)).toBe(true)
    expect(estaNoPassado(HOJE, HOJE)).toBe(false)
    expect(estaNoPassado({ ano: 2026, mes: 9, dia: 22 }, HOJE)).toBe(false)
  })
})

describe('hojeCivil', () => {
  it('deveUsarOFusoLocalEValoresDeCalendario', () => {
    expect(hojeCivil(new Date(2026, 8, 21, 23, 59))).toEqual({ ano: 2026, mes: 9, dia: 21 })
  })
})

describe('formatarDataIso', () => {
  it('deveFormatarComoDiaMesAno', () => {
    expect(formatarDataIso('1990-01-31')).toBe('31/01/1990')
  })

  it('deveDevolverOTextoOriginalQuandoNaoEUmaData', () => {
    expect(formatarDataIso('sem data')).toBe('sem data')
  })
})

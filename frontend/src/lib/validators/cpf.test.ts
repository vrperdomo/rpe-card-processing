import { describe, expect, it } from 'vitest'
import { cpfValido, formatarCpf, somenteDigitos } from './cpf'

describe('cpfValido', () => {
  it('deveAceitarCpfComDigitosVerificadoresCorretos', () => {
    expect(cpfValido('52998224725')).toBe(true)
    expect(cpfValido('111.444.777-35')).toBe(true)
  })

  it('deveAceitarCpfComOuSemMascara', () => {
    expect(cpfValido('529.982.247-25')).toBe(true)
    expect(cpfValido('52998224725')).toBe(true)
  })

  it('deveRejeitarDigitoVerificadorErrado', () => {
    expect(cpfValido('52998224726')).toBe(false)
    expect(cpfValido('52998224715')).toBe(false)
  })

  it('deveRejeitarTodosOsDigitosIguais', () => {
    for (let d = 0; d <= 9; d++) {
      expect(cpfValido(String(d).repeat(11))).toBe(false)
    }
  })

  it('deveRejeitarTamanhoErrado', () => {
    expect(cpfValido('')).toBe(false)
    expect(cpfValido('5299822472')).toBe(false)
    expect(cpfValido('529982247250')).toBe(false)
  })
})

describe('formatarCpf', () => {
  it('deveAplicarAMascaraProgressivamente', () => {
    expect(formatarCpf('5')).toBe('5')
    expect(formatarCpf('5299')).toBe('529.9')
    expect(formatarCpf('529982')).toBe('529.982')
    expect(formatarCpf('5299822')).toBe('529.982.2')
    expect(formatarCpf('529982247')).toBe('529.982.247')
    expect(formatarCpf('5299822472')).toBe('529.982.247-2')
    expect(formatarCpf('52998224725')).toBe('529.982.247-25')
  })

  it('deveIgnorarCaracteresNaoNumericosEDigitosAlemDe11', () => {
    expect(formatarCpf('abc529.982.247-25999')).toBe('529.982.247-25')
  })

  it('deveTerSomenteDigitosComoOperacaoInversa', () => {
    expect(somenteDigitos(formatarCpf('52998224725'))).toBe('52998224725')
  })
})

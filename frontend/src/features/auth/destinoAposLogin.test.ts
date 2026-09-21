import { describe, expect, it } from 'vitest'
import { destinoAposLogin } from './destinoAposLogin'

describe('destinoAposLogin', () => {
  it('deveVoltarParaARotaInternaQueOUsuarioTentouAbrir', () => {
    expect(destinoAposLogin({ from: '/portadores/123?aba=cartao' })).toBe('/portadores/123?aba=cartao')
  })

  it('deveIrParaAHomeQuandoNaoHaOrigem', () => {
    expect(destinoAposLogin(null)).toBe('/')
    expect(destinoAposLogin(undefined)).toBe('/')
    expect(destinoAposLogin({})).toBe('/')
  })

  it('naoDeveAceitarRedirecionamentoParaOutroSite', () => {
    for (const from of ['//site-malicioso.com', 'https://site-malicioso.com', '/\\site-malicioso.com', 'javascript:alert(1)']) {
      expect(destinoAposLogin({ from })).toBe('/')
    }
  })

  it('naoDeveVoltarParaALoginEmLoop', () => {
    expect(destinoAposLogin({ from: '/login' })).toBe('/')
  })

  it('deveIgnorarOrigemQueNaoETexto', () => {
    expect(destinoAposLogin({ from: 42 })).toBe('/')
  })
})

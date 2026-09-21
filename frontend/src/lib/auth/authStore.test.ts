import { describe, expect, it, vi } from 'vitest'
import { authStore } from './authStore'

describe('authStore', () => {
  it('deveComecarSemSessao', () => {
    expect(authStore.obterToken()).toBeNull()
    expect(authStore.obterEstado().motivoSaida).toBeNull()
  })

  it('deveGuardarOTokenENotificarOsOuvintes', () => {
    const ouvinte = vi.fn()
    authStore.assinar(ouvinte)

    authStore.iniciarSessao('jwt', 1800)

    expect(authStore.obterToken()).toBe('jwt')
    expect(ouvinte).toHaveBeenCalledTimes(1)
  })

  it('deveParaDeNotificarQuemCancelouAAssinatura', () => {
    const ouvinte = vi.fn()
    const cancelar = authStore.assinar(ouvinte)
    cancelar()

    authStore.iniciarSessao('jwt', 1800)

    expect(ouvinte).not.toHaveBeenCalled()
  })

  it('deveEncerrarASessaoQuandoOTokenExpira', () => {
    vi.useFakeTimers()
    authStore.iniciarSessao('jwt', 60)

    vi.advanceTimersByTime(59_000)
    expect(authStore.obterToken()).toBe('jwt')

    vi.advanceTimersByTime(1_000)
    expect(authStore.obterToken()).toBeNull()
    expect(authStore.obterEstado().motivoSaida).toBe('expirada')
  })

  it('deveCancelarOTemporizadorAoEncerrarManualmente', () => {
    vi.useFakeTimers()
    authStore.iniciarSessao('jwt', 60)

    authStore.encerrar('manual')
    vi.advanceTimersByTime(120_000)

    expect(authStore.obterEstado().motivoSaida).toBe('manual')
  })

  it('deveReiniciarOTemporizadorQuandoUmaNovaSessaoComeca', () => {
    vi.useFakeTimers()
    authStore.iniciarSessao('primeiro', 60)
    vi.advanceTimersByTime(50_000)

    authStore.iniciarSessao('segundo', 60)
    vi.advanceTimersByTime(50_000)

    expect(authStore.obterToken()).toBe('segundo')
  })

  it('naoDeveSobrescreverOMotivoQuandoNaoHaSessaoParaEncerrar', () => {
    authStore.iniciarSessao('jwt', 1800)
    authStore.encerrar('nao-autorizado')

    authStore.encerrar('manual')

    expect(authStore.obterEstado().motivoSaida).toBe('nao-autorizado')
  })

  it('deveLimparOMotivoQuandoUmaNovaSessaoComeca', () => {
    authStore.iniciarSessao('jwt', 1800)
    authStore.encerrar('expirada')

    authStore.iniciarSessao('novo', 1800)

    expect(authStore.obterEstado().motivoSaida).toBeNull()
  })

  it('naoDevePersistirOTokenEmNenhumArmazenamentoDoBrowser', () => {
    authStore.iniciarSessao('jwt-secreto', 1800)

    expect(localStorage.length).toBe(0)
    expect(sessionStorage.length).toBe(0)
    expect(document.cookie).not.toContain('jwt-secreto')
  })
})

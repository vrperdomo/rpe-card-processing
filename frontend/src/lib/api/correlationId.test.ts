import { describe, expect, it, vi } from 'vitest'
import { novoCorrelationId } from './correlationId'

const UUID_V4 = /^[0-9a-f]{8}-[0-9a-f]{4}-4[0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$/

describe('novoCorrelationId', () => {
  it('deveGerarUuidV4', () => {
    expect(novoCorrelationId()).toMatch(UUID_V4)
  })

  it('deveGerarUuidV4TambemQuandoRandomUuidNaoExisteEmContextoInseguro', () => {
    // Simula http://<IP>:3000, onde só getRandomValues está disponível.
    vi.stubGlobal('crypto', { getRandomValues: crypto.getRandomValues.bind(crypto) })

    const primeiro = novoCorrelationId()
    const segundo = novoCorrelationId()

    expect(primeiro).toMatch(UUID_V4)
    expect(primeiro).not.toBe(segundo)
    vi.unstubAllGlobals()
  })
})

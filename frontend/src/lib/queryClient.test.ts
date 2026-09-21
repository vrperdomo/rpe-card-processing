import { describe, expect, it } from 'vitest'
import { ApiError } from './api/apiError'
import { deveTentarNovamente } from './queryClient'

describe('deveTentarNovamente', () => {
  it('naoDeveRepetirRespostaDefinitivaDoBackend', () => {
    for (const status of [400, 401, 404, 409, 422, 429]) {
      expect(deveTentarNovamente(0, new ApiError({ status }))).toBe(false)
    }
  })

  it('deveRepetirFalhaDeRedeEErroDeServidor', () => {
    expect(deveTentarNovamente(0, new ApiError({ status: 0 }))).toBe(true)
    expect(deveTentarNovamente(0, new ApiError({ status: 503 }))).toBe(true)
    expect(deveTentarNovamente(1, new ApiError({ status: 500 }))).toBe(true)
  })

  it('deveDesistirDepoisDeDuasRepeticoes', () => {
    expect(deveTentarNovamente(2, new ApiError({ status: 503 }))).toBe(false)
  })
})

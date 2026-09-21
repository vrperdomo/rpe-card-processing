import { describe, expect, it } from 'vitest'
import { ApiError } from './apiError'
import { codigoDeSuporte, mensagemDeErro } from './mensagemDeErro'

describe('mensagemDeErro', () => {
  it('deveUsarODetalheDoBackendEmErroDeCliente', () => {
    const erro = new ApiError({ status: 422, detalhe: 'Portador menor de 18 anos' })

    expect(mensagemDeErro(erro)).toBe('Portador menor de 18 anos')
  })

  it('deveCairNoTituloQuandoNaoHaDetalhe', () => {
    const erro = new ApiError({ status: 409, titulo: 'Conflito' })

    expect(mensagemDeErro(erro)).toBe('Conflito')
  })

  it('deveInformarOTempoDeEsperaEm429', () => {
    expect(mensagemDeErro(new ApiError({ status: 429, retryAfterSegundos: 42 }))).toBe(
      'Muitas tentativas. Tente novamente em 42 segundos.',
    )
    expect(mensagemDeErro(new ApiError({ status: 429, retryAfterSegundos: 1 }))).toBe(
      'Muitas tentativas. Tente novamente em 1 segundo.',
    )
  })

  it('deveTerMensagemGenericaEm429SemRetryAfter', () => {
    expect(mensagemDeErro(new ApiError({ status: 429 }))).toBe(
      'Muitas tentativas. Aguarde um pouco e tente novamente.',
    )
  })

  it('deveExplicarServicoIndisponivelEm503', () => {
    expect(mensagemDeErro(new ApiError({ status: 503 }))).toContain('temporariamente indisponível')
  })

  it('naoDeveVazarDetalheInternoDeErro5xx', () => {
    const erro = new ApiError({ status: 500, detalhe: 'NullPointerException em PortadorService:42' })

    expect(mensagemDeErro(erro)).not.toContain('NullPointerException')
    expect(mensagemDeErro(erro)).toContain('Erro inesperado no servidor')
  })

  it('deveExplicarFalhaDeConexao', () => {
    expect(mensagemDeErro(new ApiError({ status: 0 }))).toContain('conectar ao servidor')
  })

  it('deveTratarErroDesconhecidoSemExporSuaMensagem', () => {
    expect(mensagemDeErro(new Error('detalhe interno'))).toBe(
      'Ocorreu um erro inesperado. Tente novamente.',
    )
  })
})

describe('codigoDeSuporte', () => {
  it('deveExporOCorrelationIdDoErroDaApi', () => {
    expect(codigoDeSuporte(new ApiError({ status: 500, correlationId: 'id-1' }))).toBe('id-1')
  })

  it('deveRetornarUndefinedParaOutrosErros', () => {
    expect(codigoDeSuporte(new Error('x'))).toBeUndefined()
  })
})

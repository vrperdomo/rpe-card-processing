import { describe, expect, it } from 'vitest'
import { ApiError } from '../../lib/api/apiError'
import { camposAfetados } from './errosDoServidor'

describe('camposAfetados', () => {
  it('deveMapearErrosDeValidacaoParaOsCamposDoFormulario', () => {
    const erro = new ApiError({
      status: 400,
      errosDeCampo: [
        { field: 'cpf', message: 'CPF inválido' },
        { field: 'dataNascimento', message: 'deve ser uma data passada' },
      ],
    })

    expect(camposAfetados(erro)).toEqual([
      { campo: 'cpf', mensagem: 'CPF inválido' },
      { campo: 'dataNascimento', mensagem: 'deve ser uma data passada' },
    ])
  })

  it('deveIgnorarCamposQueNaoExistemNoFormulario', () => {
    const erro = new ApiError({ status: 400, errosDeCampo: [{ field: 'outraCoisa', message: 'x' }] })

    expect(camposAfetados(erro)).toEqual([])
  })

  it('deveApontarOCpfQuandoHaConflito409', () => {
    const erro = new ApiError({ status: 409, detalhe: 'Já existe um portador cadastrado com este CPF' })

    expect(camposAfetados(erro)).toEqual([
      { campo: 'cpf', mensagem: 'Já existe um portador cadastrado com este CPF' },
    ])
  })

  it('naoDeveDuplicarOCpfQuandoOServidorJaApontouOCampo', () => {
    const erro = new ApiError({ status: 409, errosDeCampo: [{ field: 'cpf', message: 'duplicado' }] })

    expect(camposAfetados(erro)).toHaveLength(1)
  })

  it('deveRetornarVazioParaErrosQueNaoSaoDeCampo', () => {
    expect(camposAfetados(new ApiError({ status: 422, detalhe: 'Produto não está ATIVO' }))).toEqual([])
    expect(camposAfetados(new ApiError({ status: 503 }))).toEqual([])
    expect(camposAfetados(new Error('x'))).toEqual([])
    expect(camposAfetados(null)).toEqual([])
  })
})

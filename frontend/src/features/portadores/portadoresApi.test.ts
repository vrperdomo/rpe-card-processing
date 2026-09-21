import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { CPF_VALIDO, ID_PORTADOR, ID_PRODUTO, portadorCompleto, portadorResponse } from '../../test/fixtures'
import { espiarFetch, respostaJson } from '../../test/utils'
import { buscarPortadorCompleto, cadastrarPortador, cadastroPortadorSchema } from './portadoresApi'

const formValido = {
  nome: 'Victor Rodrigues',
  cpf: CPF_VALIDO,
  dataNascimento: '1990-01-31',
  produtoId: ID_PRODUTO,
}

function mensagens(entrada: Record<string, unknown>): Record<string, string> {
  const resultado = cadastroPortadorSchema.safeParse(entrada)
  if (resultado.success) return {}
  // Como o React Hook Form, fica a primeira mensagem de cada campo.
  const primeiras: Record<string, string> = {}
  for (const issue of resultado.error.issues) {
    const campo = String(issue.path[0])
    if (!(campo in primeiras)) primeiras[campo] = issue.message
  }
  return primeiras
}

describe('cadastroPortadorSchema', () => {
  // Fixa "hoje" em 21/09/2026 para as regras de idade não dependerem do dia em que o teste roda.
  beforeEach(() => {
    vi.useFakeTimers()
    vi.setSystemTime(new Date(2026, 8, 21, 12, 0, 0))
  })
  afterEach(() => vi.useRealTimers())

  it('deveAceitarUmCadastroValido', () => {
    expect(cadastroPortadorSchema.safeParse(formValido).success).toBe(true)
  })

  it('deveExigirTodosOsCampos', () => {
    expect(mensagens({ nome: '', cpf: '', dataNascimento: '', produtoId: '' })).toEqual({
      nome: 'Informe o nome',
      cpf: 'Informe o CPF',
      dataNascimento: 'Informe a data de nascimento',
      produtoId: 'Selecione um produto',
    })
  })

  it('deveRejeitarNomeSoComEspacosOuMuitoLongo', () => {
    expect(mensagens({ ...formValido, nome: '   ' }).nome).toBe('Informe o nome')
    expect(mensagens({ ...formValido, nome: 'a'.repeat(151) }).nome).toContain('150')
  })

  it('deveRejeitarCpfInvalido', () => {
    expect(mensagens({ ...formValido, cpf: '529.982.247-26' }).cpf).toBe('CPF inválido')
  })

  it('deveAceitarQuemFazE18AnosHoje', () => {
    expect(mensagens({ ...formValido, dataNascimento: '2008-09-21' })).toEqual({})
  })

  it('deveRejeitarQuemFaz18AnosAmanha', () => {
    expect(mensagens({ ...formValido, dataNascimento: '2008-09-22' }).dataNascimento).toBe(
      'É necessário ter 18 anos ou mais',
    )
  })

  it('deveRejeitarDataDeHojeEDataFutura', () => {
    expect(mensagens({ ...formValido, dataNascimento: '2026-09-21' }).dataNascimento).toContain('passado')
    expect(mensagens({ ...formValido, dataNascimento: '2027-01-01' }).dataNascimento).toContain('passado')
  })

  it('deveRejeitarDataInexistente', () => {
    expect(mensagens({ ...formValido, dataNascimento: '1990-02-31' }).dataNascimento).toBe('Data inválida')
  })

  it('deveRejeitarProdutoQueNaoEUmIdentificador', () => {
    expect(mensagens({ ...formValido, produtoId: 'nao-e-uuid' }).produtoId).toBe('Selecione um produto')
  })
})

describe('cadastrarPortador', () => {
  it('deveEnviarSoOsDigitosDoCpfENomeSemEspacosNasPontas', async () => {
    const fetchSpy = espiarFetch().mockResolvedValue(respostaJson(201, portadorResponse()))

    await cadastrarPortador({ ...formValido, nome: '  Victor Rodrigues  ' })

    const corpo = JSON.parse(String(fetchSpy.mock.calls[0]?.[1]?.body)) as Record<string, string>
    expect(corpo).toEqual({
      nome: 'Victor Rodrigues',
      cpf: '52998224725',
      dataNascimento: '1990-01-31',
      produtoId: ID_PRODUTO,
    })
    expect(fetchSpy.mock.calls[0]?.[0]).toBe('/api/v1/portadores')
    expect(fetchSpy.mock.calls[0]?.[1]?.method).toBe('POST')
  })

  it('deveRejeitarRespostaForaDoContrato', async () => {
    espiarFetch().mockResolvedValue(respostaJson(201, { id: 'x' }))

    await expect(cadastrarPortador(formValido)).rejects.toThrow()
  })
})

describe('buscarPortadorCompleto', () => {
  it('deveNormalizarCartaoEProdutoAusentesParaNull', async () => {
    espiarFetch().mockResolvedValue(
      respostaJson(200, { portador: portadorResponse(), emissao: 'PENDENTE' }),
    )

    const resultado = await buscarPortadorCompleto(ID_PORTADOR)

    expect(resultado.cartao).toBeNull()
    expect(resultado.produto).toBeNull()
    expect(resultado.avisos).toEqual([])
  })

  it('deveTratarStatusDeEmissaoDesconhecidoComoDesconhecida', async () => {
    espiarFetch().mockResolvedValue(respostaJson(200, portadorCompleto({ emissao: 'ALGO_NOVO' })))

    const resultado = await buscarPortadorCompleto(ID_PORTADOR)

    expect(resultado.emissao).toBe('DESCONHECIDA')
  })

  it('deveCodificarOIdentificadorNaUrl', async () => {
    const fetchSpy = espiarFetch().mockResolvedValue(respostaJson(200, portadorCompleto()))

    await buscarPortadorCompleto('a/b?c')

    expect(fetchSpy.mock.calls[0]?.[0]).toBe('/api/v1/portadores/a%2Fb%3Fc/completo')
  })
})

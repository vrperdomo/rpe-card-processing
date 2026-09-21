import { describe, expect, it } from 'vitest'
import { cabecalhosDaChamada, espiarFetch, respostaJson } from '../../test/utils'
import { authStore } from '../auth/authStore'
import { ApiError } from './apiError'
import { apiFetch } from './client'

async function capturarErro(promessa: Promise<unknown>): Promise<ApiError> {
  try {
    await promessa
  } catch (erro) {
    if (erro instanceof ApiError) return erro
    throw erro
  }
  throw new Error('a requisição deveria ter falhado')
}

describe('apiFetch', () => {
  it('deveEnviarOTokenQuandoHaSessao', async () => {
    const fetchSpy = espiarFetch().mockResolvedValue(respostaJson(200, { ok: true }))
    authStore.iniciarSessao('jwt-da-sessao', 1800)

    await apiFetch('/api/v1/produtos')

    expect(cabecalhosDaChamada(fetchSpy, 0)['Authorization']).toBe('Bearer jwt-da-sessao')
  })

  it('naoDeveEnviarOTokenEmRotaPublica', async () => {
    const fetchSpy = espiarFetch().mockResolvedValue(respostaJson(200, {}))
    authStore.iniciarSessao('jwt-da-sessao', 1800)

    await apiFetch('/api/v1/auth/login', { metodo: 'POST', corpo: {}, publica: true })

    expect(cabecalhosDaChamada(fetchSpy, 0)['Authorization']).toBeUndefined()
  })

  it('deveEnviarUmCorrelationIdNovoACadaRequisicao', async () => {
    const fetchSpy = espiarFetch().mockImplementation(async () => respostaJson(200, {}))

    await apiFetch('/api/v1/produtos')
    await apiFetch('/api/v1/produtos')

    const primeiro = cabecalhosDaChamada(fetchSpy, 0)['X-Correlation-Id']
    const segundo = cabecalhosDaChamada(fetchSpy, 1)['X-Correlation-Id']
    expect(primeiro).toBeTruthy()
    expect(primeiro).not.toBe(segundo)
  })

  it('deveEnviarOCorpoComoJsonECabecalhoContentType', async () => {
    const fetchSpy = espiarFetch().mockResolvedValue(respostaJson(201, {}))

    await apiFetch('/api/v1/portadores', { metodo: 'POST', corpo: { nome: 'Ana' } })

    const init = fetchSpy.mock.calls[0]?.[1]
    expect(init?.body).toBe('{"nome":"Ana"}')
    expect(cabecalhosDaChamada(fetchSpy, 0)['Content-Type']).toBe('application/json')
  })

  it('deveConverterProblemDetailEmApiError', async () => {
    espiarFetch().mockResolvedValue(
      respostaJson(
        422,
        { title: 'Regra de negócio violada', detail: 'Portador menor de 18 anos', correlationId: 'abc-123' },
        { 'Retry-After': '30' },
      ),
    )

    const erro = await capturarErro(apiFetch('/api/v1/portadores'))

    expect(erro.status).toBe(422)
    expect(erro.titulo).toBe('Regra de negócio violada')
    expect(erro.detalhe).toBe('Portador menor de 18 anos')
    expect(erro.correlationId).toBe('abc-123')
    expect(erro.retryAfterSegundos).toBe(30)
  })

  it('deveExporOsErrosDeCampoDeUmaValidacao400', async () => {
    espiarFetch().mockResolvedValue(
      respostaJson(400, {
        title: 'Payload inválido',
        errors: [{ field: 'cpf', message: 'CPF inválido' }, { campo: 'sem-formato' }, 'texto', null],
      }),
    )

    const erro = await capturarErro(apiFetch('/api/v1/portadores', { metodo: 'POST', corpo: {} }))

    expect(erro.errosDeCampo).toEqual([{ field: 'cpf', message: 'CPF inválido' }])
  })

  it('deveIgnorarErrosDeCampoQueNaoSaoUmaLista', async () => {
    espiarFetch().mockResolvedValue(respostaJson(400, { errors: 'quebrado' }))

    const erro = await capturarErro(apiFetch('/api/v1/portadores'))

    expect(erro.errosDeCampo).toEqual([])
  })

  it('deveIgnorarCorpoQueNaoEJson', async () => {
    espiarFetch().mockResolvedValue(
      new Response('<html>502 Bad Gateway</html>', { status: 502, headers: { 'Content-Type': 'text/html' } }),
    )

    const erro = await capturarErro(apiFetch('/api/v1/produtos'))

    expect(erro.status).toBe(502)
    expect(erro.detalhe).toBeUndefined()
  })

  it('deveEncerrarASessaoQuandoORecursoRetorna401ComToken', async () => {
    espiarFetch().mockResolvedValue(respostaJson(401, { title: 'Não autenticado' }))
    authStore.iniciarSessao('jwt-vencido', 1800)

    await capturarErro(apiFetch('/api/v1/produtos'))

    expect(authStore.obterToken()).toBeNull()
    expect(authStore.obterEstado().motivoSaida).toBe('nao-autorizado')
  })

  it('naoDeveEncerrarASessaoQuandoUmaRotaPublicaRetorna401', async () => {
    espiarFetch().mockResolvedValue(respostaJson(401, { detail: 'Usuário ou senha inválidos' }))
    authStore.iniciarSessao('jwt-valido', 1800)

    await capturarErro(apiFetch('/api/v1/auth/login', { metodo: 'POST', publica: true }))

    expect(authStore.obterToken()).toBe('jwt-valido')
  })

  it('deveSinalizarFalhaDeRedeComStatusZero', async () => {
    espiarFetch().mockRejectedValue(new TypeError('Failed to fetch'))

    const erro = await capturarErro(apiFetch('/api/v1/produtos'))

    expect(erro.status).toBe(0)
    expect(erro.semConexao).toBe(true)
  })

  it('deveRepassarOAbortSemTransformarEmErroDeRede', async () => {
    espiarFetch().mockRejectedValue(new DOMException('cancelado', 'AbortError'))

    await expect(apiFetch('/api/v1/produtos')).rejects.toMatchObject({ name: 'AbortError' })
  })

  it('deveRetornarUndefinedEmRespostaSemConteudo', async () => {
    espiarFetch().mockResolvedValue(new Response(null, { status: 204 }))

    await expect(apiFetch('/api/v1/algo', { metodo: 'DELETE' })).resolves.toBeUndefined()
  })
})

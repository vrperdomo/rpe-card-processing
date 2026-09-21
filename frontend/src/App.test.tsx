import { act, screen, waitFor } from '@testing-library/react'
import { describe, expect, it } from 'vitest'
import { authStore } from './lib/auth/authStore'
import {
  cabecalhosDaChamada,
  espiarFetch,
  renderApp,
  RESPOSTA_LOGIN_OK,
  respostaJson,
} from './test/utils'

async function preencherEEnviar(
  user: ReturnType<typeof renderApp>['user'],
  usuario: string,
  senha: string,
) {
  if (usuario) await user.type(screen.getByLabelText('Usuário'), usuario)
  if (senha) await user.type(screen.getByLabelText('Senha'), senha)
  await user.click(screen.getByRole('button', { name: 'Entrar' }))
}

describe('rotas protegidas', () => {
  it('deveRedirecionarParaOLoginQuandoNaoHaSessao', () => {
    renderApp('/')

    expect(screen.getByLabelText('Usuário')).toBeInTheDocument()
    expect(screen.queryByRole('heading', { name: 'Portadores' })).not.toBeInTheDocument()
  })

  it('deveRedirecionarRotasDesconhecidasParaOLoginQuandoNaoHaSessao', () => {
    renderApp('/nao-existe')

    expect(screen.getByLabelText('Usuário')).toBeInTheDocument()
  })

  it('deveMostrarAAreaPrivadaQuandoHaSessao', () => {
    authStore.iniciarSessao('jwt', 1800)

    renderApp('/')

    expect(screen.getByRole('heading', { name: 'Portadores' })).toBeInTheDocument()
  })

  it('deveMandarQuemJaEstaAutenticadoDoLoginParaAHome', () => {
    authStore.iniciarSessao('jwt', 1800)

    renderApp('/login')

    expect(screen.getByRole('heading', { name: 'Portadores' })).toBeInTheDocument()
  })
})

describe('login', () => {
  it('deveValidarOsCamposAntesDeChamarOBackend', async () => {
    const fetchSpy = espiarFetch()
    const { user } = renderApp('/login')

    await preencherEEnviar(user, '', '')

    expect(await screen.findByText('Informe o usuário')).toBeInTheDocument()
    expect(screen.getByText('Informe a senha')).toBeInTheDocument()
    expect(screen.getByLabelText('Usuário')).toHaveAttribute('aria-invalid', 'true')
    expect(fetchSpy).not.toHaveBeenCalled()
  })

  it('deveEntrarEMostrarAHomeQuandoAsCredenciaisSaoValidas', async () => {
    const fetchSpy = espiarFetch().mockResolvedValue(respostaJson(200, RESPOSTA_LOGIN_OK))
    const { user } = renderApp('/login')

    await preencherEEnviar(user, 'admin', 'admin123')

    expect(await screen.findByRole('heading', { name: 'Portadores' })).toBeInTheDocument()
    expect(fetchSpy).toHaveBeenCalledTimes(1)
    expect(fetchSpy.mock.calls[0]?.[0]).toBe('/api/v1/auth/login')
    expect(fetchSpy.mock.calls[0]?.[1]?.method).toBe('POST')
    expect(fetchSpy.mock.calls[0]?.[1]?.body).toBe('{"username":"admin","password":"admin123"}')
    expect(cabecalhosDaChamada(fetchSpy, 0)['Authorization']).toBeUndefined()
    expect(authStore.obterToken()).toBe('jwt-de-teste')
  })

  it('deveVoltarParaARotaQueOUsuarioTentouAbrirDepoisDoLogin', async () => {
    espiarFetch().mockResolvedValue(respostaJson(200, RESPOSTA_LOGIN_OK))
    const { user } = renderApp('/?filtro=ativos')

    await preencherEEnviar(user, 'admin', 'admin123')

    expect(await screen.findByRole('heading', { name: 'Portadores' })).toBeInTheDocument()
  })

  it('naoDeveGuardarOTokenEmNenhumArmazenamentoDoBrowser', async () => {
    espiarFetch().mockResolvedValue(respostaJson(200, RESPOSTA_LOGIN_OK))
    const { user } = renderApp('/login')

    await preencherEEnviar(user, 'admin', 'admin123')
    await screen.findByRole('heading', { name: 'Portadores' })

    expect(localStorage.length).toBe(0)
    expect(sessionStorage.length).toBe(0)
    expect(document.cookie).toBe('')
  })

  it('deveMostrarErroLimparASenhaEDevolverOFocoQuandoCredenciaisInvalidas', async () => {
    espiarFetch().mockResolvedValue(
      respostaJson(401, { title: 'Não autenticado', detail: 'Usuário ou senha inválidos', correlationId: 'corr-401' }),
    )
    const { user } = renderApp('/login')

    await preencherEEnviar(user, 'admin', 'errada')

    expect(await screen.findByText('Usuário ou senha inválidos')).toBeInTheDocument()
    expect(screen.getByText('Código de suporte: corr-401')).toBeInTheDocument()
    await waitFor(() => expect(screen.getByLabelText('Senha')).toHaveValue(''))
    expect(screen.getByLabelText('Senha')).toHaveFocus()
    expect(screen.getByLabelText('Usuário')).toHaveValue('admin')
    expect(authStore.obterToken()).toBeNull()
  })

  it('deveInformarOTempoDeEsperaQuandoOLimiteDeTentativasEstoura', async () => {
    espiarFetch().mockResolvedValue(
      respostaJson(429, { title: 'Muitas tentativas' }, { 'Retry-After': '42' }),
    )
    const { user } = renderApp('/login')

    await preencherEEnviar(user, 'admin', 'errada')

    expect(await screen.findByText('Muitas tentativas. Tente novamente em 42 segundos.')).toBeInTheDocument()
  })

  it('deveExplicarQuandoOServicoEstaIndisponivel', async () => {
    espiarFetch().mockResolvedValue(respostaJson(503, { title: 'Serviço indisponível' }))
    const { user } = renderApp('/login')

    await preencherEEnviar(user, 'admin', 'admin123')

    expect(await screen.findByText(/temporariamente indisponível/)).toBeInTheDocument()
  })

  it('deveExplicarQuandoNaoHaConexaoComOServidor', async () => {
    espiarFetch().mockRejectedValue(new TypeError('Failed to fetch'))
    const { user } = renderApp('/login')

    await preencherEEnviar(user, 'admin', 'admin123')

    expect(await screen.findByText(/conectar ao servidor/)).toBeInTheDocument()
  })

  it('deveDesabilitarOBotaoEnquantoOLoginEstaEmAndamento', async () => {
    let concluir: (resposta: Response) => void = () => {}
    espiarFetch().mockImplementation(
      () =>
        new Promise<Response>((resolve) => {
          concluir = resolve
        }),
    )
    const { user } = renderApp('/login')

    await preencherEEnviar(user, 'admin', 'admin123')

    const botao = await screen.findByRole('button', { name: 'Entrando…' })
    expect(botao).toBeDisabled()

    await act(async () => concluir(respostaJson(200, RESPOSTA_LOGIN_OK)))
    expect(await screen.findByRole('heading', { name: 'Portadores' })).toBeInTheDocument()
  })

  it('deveRejeitarRespostaDeLoginForaDoContrato', async () => {
    espiarFetch().mockResolvedValue(respostaJson(200, { token: 'formato-antigo' }))
    const { user } = renderApp('/login')

    await preencherEEnviar(user, 'admin', 'admin123')

    expect(await screen.findByText('Ocorreu um erro inesperado. Tente novamente.')).toBeInTheDocument()
    expect(authStore.obterToken()).toBeNull()
  })
})

describe('encerramento da sessão', () => {
  it('deveVoltarAoLoginSemAvisoQuandoOUsuarioClicaEmSair', async () => {
    authStore.iniciarSessao('jwt', 1800)
    const { user } = renderApp('/')

    await user.click(screen.getByRole('button', { name: 'Sair' }))

    expect(await screen.findByLabelText('Usuário')).toBeInTheDocument()
    expect(screen.queryByRole('status')).not.toBeInTheDocument()
    expect(authStore.obterToken()).toBeNull()
  })

  it('deveEsvaziarOCacheDeDadosAoSair', async () => {
    authStore.iniciarSessao('jwt', 1800)
    const { user, queryClient } = renderApp('/')
    queryClient.setQueryData(['portadores'], [{ id: 1 }])

    await user.click(screen.getByRole('button', { name: 'Sair' }))

    expect(queryClient.getQueryData(['portadores'])).toBeUndefined()
  })

  it('deveAvisarQuandoASessaoExpira', async () => {
    authStore.iniciarSessao('jwt', 1800)
    renderApp('/')

    act(() => authStore.encerrar('expirada'))

    expect(await screen.findByRole('status')).toHaveTextContent('Sua sessão expirou')
    expect(screen.getByLabelText('Usuário')).toBeInTheDocument()
  })

  it('deveAvisarQuandoOServidorRejeitaOToken', async () => {
    authStore.iniciarSessao('jwt', 1800)
    renderApp('/')

    act(() => authStore.encerrar('nao-autorizado'))

    expect(await screen.findByRole('status')).toHaveTextContent('não é mais válida')
  })
})

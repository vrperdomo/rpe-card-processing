import { act, screen, waitFor } from '@testing-library/react'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { authStore } from '../../lib/auth/authStore'
import {
  chamadasPara,
  CARTAO_EMITIDO,
  ID_PORTADOR,
  portadorCompleto,
  rotaCompleto,
  rotearFetch,
} from '../../test/fixtures'
import { renderApp, respostaJson } from '../../test/utils'
import { POLLING } from './polling'

const CAMINHO = `/api/v1/portadores/${ID_PORTADOR}/completo`

// Relógio falso que também avança em tempo real, para o waitFor/findBy do Testing Library seguir
// funcionando; o polling é acelerado com advanceTimersByTimeAsync.
function comRelogioFalso() {
  vi.useFakeTimers({ shouldAdvanceTime: true })
}

async function avancar(ms: number) {
  await act(async () => {
    await vi.advanceTimersByTimeAsync(ms)
  })
}

beforeEach(() => authStore.iniciarSessao('jwt', 1800))

describe('detalhe do portador', () => {
  it('deveMostrarPortadorProdutoECartaoQuandoAEmissaoConcluiu', async () => {
    rotearFetch([rotaCompleto(() => portadorCompleto({ emissao: 'CONCLUIDA', cartao: CARTAO_EMITIDO }))])

    renderApp(`/portadores/${ID_PORTADOR}`)

    expect(await screen.findByRole('heading', { name: 'Victor Rodrigues' })).toBeInTheDocument()
    expect(screen.getByText('Cartão emitido')).toBeInTheDocument()
    expect(screen.getByText('**** **** **** 6707')).toBeInTheDocument()
    expect(screen.getByText('09/31')).toBeInTheDocument()
    expect(screen.getByText('***.982.247-**')).toBeInTheDocument()
    expect(screen.getByText('31/01/1990')).toBeInTheDocument()
    expect(screen.getByText('Gold Teste')).toBeInTheDocument()
  })

  it('deveExibirOCpfExatamenteComoAApiMascarou', async () => {
    rotearFetch([rotaCompleto(() => portadorCompleto({ emissao: 'CONCLUIDA', cartao: CARTAO_EMITIDO }))])

    renderApp(`/portadores/${ID_PORTADOR}`)

    await screen.findByRole('heading', { name: 'Victor Rodrigues' })
    // Nenhum CPF completo (11 dígitos) aparece na tela.
    expect(document.body.textContent).not.toMatch(/\d{3}\.?\d{3}\.?\d{3}-?\d{2}/)
  })

  it('deveAtualizarSozinhoAteAEmissaoConcluir', async () => {
    comRelogioFalso()
    let respostas = 0
    const fetchSpy = rotearFetch([
      rotaCompleto(() => {
        respostas += 1
        return respostas < 3
          ? portadorCompleto({ emissao: 'PENDENTE' })
          : portadorCompleto({ emissao: 'CONCLUIDA', cartao: CARTAO_EMITIDO })
      }),
    ])

    renderApp(`/portadores/${ID_PORTADOR}`)
    expect(await screen.findByText('Emissão em andamento')).toBeInTheDocument()
    expect(screen.getByText('Ainda não emitido')).toBeInTheDocument()

    await avancar(POLLING.pendenteMs)
    await avancar(POLLING.pendenteMs)

    expect(await screen.findByText('Cartão emitido')).toBeInTheDocument()
    expect(screen.getByText('**** **** **** 6707')).toBeInTheDocument()
    expect(chamadasPara(fetchSpy, CAMINHO)).toHaveLength(3)
  })

  it('deveParaDeConsultarDepoisQueAEmissaoConclui', async () => {
    comRelogioFalso()
    const fetchSpy = rotearFetch([rotaCompleto(() => portadorCompleto({ emissao: 'CONCLUIDA', cartao: CARTAO_EMITIDO }))])

    renderApp(`/portadores/${ID_PORTADOR}`)
    await screen.findByText('Cartão emitido')
    await avancar(POLLING.pendenteMs * 5)

    expect(chamadasPara(fetchSpy, CAMINHO)).toHaveLength(1)
  })

  it('deveDesistirDoPollingEExplicarQuandoAEmissaoDemoraDemais', async () => {
    comRelogioFalso()
    const fetchSpy = rotearFetch([rotaCompleto(() => portadorCompleto({ emissao: 'PENDENTE' }))])

    renderApp(`/portadores/${ID_PORTADOR}`)
    await screen.findByText('Emissão em andamento')
    await avancar(POLLING.limiteMs + POLLING.pendenteMs)

    expect(await screen.findByText(/demorando mais que o esperado/)).toBeInTheDocument()
    const chamadasNoLimite = chamadasPara(fetchSpy, CAMINHO).length
    await avancar(POLLING.pendenteMs * 10)
    expect(chamadasPara(fetchSpy, CAMINHO)).toHaveLength(chamadasNoLimite)
  })

  it('deveConsultarDeNovoQuandoOUsuarioClicaEmAtualizarMesmoDepoisDoLimite', async () => {
    comRelogioFalso()
    const fetchSpy = rotearFetch([rotaCompleto(() => portadorCompleto({ emissao: 'PENDENTE' }))])

    const { user } = renderApp(`/portadores/${ID_PORTADOR}`)
    await screen.findByText('Emissão em andamento')
    await avancar(POLLING.limiteMs + POLLING.pendenteMs)
    await screen.findByText(/demorando mais que o esperado/)
    const antes = chamadasPara(fetchSpy, CAMINHO).length

    await user.click(screen.getByRole('button', { name: 'Atualizar' }))

    await waitFor(() => expect(chamadasPara(fetchSpy, CAMINHO).length).toBe(antes + 1))
  })

  it('deveMostrarOsAvisosEProdutoIndisponivelNaRespostaDegradada', async () => {
    rotearFetch([
      rotaCompleto(() =>
        portadorCompleto({
          emissao: 'DESCONHECIDA',
          produto: null,
          avisos: ['Cartão indisponível no momento', 'Produto indisponível no momento'],
        }),
      ),
    ])

    renderApp(`/portadores/${ID_PORTADOR}`)

    expect(await screen.findByText('Situação da emissão indisponível')).toBeInTheDocument()
    expect(screen.getByText('Cartão indisponível no momento')).toBeInTheDocument()
    expect(screen.getByText('Produto indisponível no momento', { selector: 'li' })).toBeInTheDocument()
    expect(screen.getByText('Indisponível no momento')).toBeInTheDocument()
    // Os dados do portador continuam visíveis: a degradação não derruba a tela.
    expect(screen.getByRole('heading', { name: 'Victor Rodrigues' })).toBeInTheDocument()
  })

  it('deveConsultarMaisDevagarQuandoOServicoDeCartoesNaoResponde', async () => {
    comRelogioFalso()
    const fetchSpy = rotearFetch([rotaCompleto(() => portadorCompleto({ emissao: 'DESCONHECIDA', avisos: ['Cartão indisponível no momento'] }))])

    renderApp(`/portadores/${ID_PORTADOR}`)
    await screen.findByText('Situação da emissão indisponível')
    await avancar(POLLING.pendenteMs)
    expect(chamadasPara(fetchSpy, CAMINHO)).toHaveLength(1)

    await avancar(POLLING.desconhecidaMs)
    await waitFor(() => expect(chamadasPara(fetchSpy, CAMINHO).length).toBeGreaterThanOrEqual(2))
  })

  it('deveManterOsUltimosDadosEAvisarQuandoAAtualizacaoFalha', async () => {
    comRelogioFalso()
    let chamada = 0
    rotearFetch([
      {
        caminho: CAMINHO,
        resposta: () => {
          chamada += 1
          return chamada === 1
            ? respostaJson(200, portadorCompleto({ emissao: 'PENDENTE' }))
            : respostaJson(503, { title: 'Serviço indisponível' })
        },
      },
    ])

    renderApp(`/portadores/${ID_PORTADOR}`)
    await screen.findByText('Emissão em andamento')
    await avancar(POLLING.pendenteMs)
    // 503 é transitório: o React Query repete (1 s e 2 s) antes de dar o erro como definitivo.
    await avancar(3_000)

    expect(await screen.findByText(/Mostrando os últimos dados conhecidos/)).toBeInTheDocument()
    expect(screen.getByRole('heading', { name: 'Victor Rodrigues' })).toBeInTheDocument()
  })
})

describe('detalhe do portador: erros', () => {
  it('deveInformarQuandoOPortadorNaoExisteSemOferecerNovaTentativa', async () => {
    rotearFetch([rotaCompleto(() => ({ title: 'Recurso não encontrado', detail: 'Portador não encontrado' }), 404)])

    renderApp(`/portadores/${ID_PORTADOR}`)

    expect(await screen.findByRole('heading', { name: 'Portador não encontrado' })).toBeInTheDocument()
    expect(screen.queryByRole('button', { name: 'Tentar novamente' })).not.toBeInTheDocument()
  })

  it('deveOferecerNovaTentativaQuandoOServicoEstaIndisponivel', async () => {
    let indisponivel = true
    rotearFetch([
      {
        caminho: CAMINHO,
        resposta: () =>
          indisponivel
            ? respostaJson(422, { title: 'Erro', detail: 'Falha temporária', correlationId: 'corr-x' })
            : respostaJson(200, portadorCompleto({ emissao: 'CONCLUIDA', cartao: CARTAO_EMITIDO })),
      },
    ])

    const { user } = renderApp(`/portadores/${ID_PORTADOR}`)

    expect(await screen.findByRole('heading', { name: 'Não foi possível carregar o portador' })).toBeInTheDocument()
    expect(screen.getByText('Código de suporte: corr-x')).toBeInTheDocument()

    indisponivel = false
    await user.click(screen.getByRole('button', { name: 'Tentar novamente' }))

    expect(await screen.findByText('Cartão emitido')).toBeInTheDocument()
  })

  it('naoDeveChamarOBackendComIdentificadorInvalido', async () => {
    const fetchSpy = rotearFetch([])

    renderApp('/portadores/isso-nao-e-um-uuid')

    expect(await screen.findByRole('heading', { name: 'Identificador inválido' })).toBeInTheDocument()
    expect(fetchSpy).not.toHaveBeenCalled()
  })
})

import { screen, waitFor, within } from '@testing-library/react'
import { beforeEach, describe, expect, it } from 'vitest'
import { authStore } from '../../lib/auth/authStore'
import {
  chamadasPara,
  CPF_VALIDO,
  ID_PORTADOR,
  paginaDeProdutos,
  portadorCompleto,
  portadorResponse,
  rotaCompleto,
  rotaProdutos,
  rotearFetch,
} from '../../test/fixtures'
import { renderApp, respostaJson } from '../../test/utils'

type Usuario = ReturnType<typeof renderApp>['user']

const ROTA_CADASTRO = { metodo: 'POST', caminho: '/api/v1/portadores' }

async function preencher(user: Usuario, campos: { nome?: string; cpf?: string; nascimento?: string; produto?: string }) {
  if (campos.nome) await user.type(screen.getByLabelText('Nome completo'), campos.nome)
  if (campos.cpf) await user.type(screen.getByLabelText('CPF'), campos.cpf)
  if (campos.nascimento) await user.type(screen.getByLabelText('Data de nascimento'), campos.nascimento)
  if (campos.produto) await user.selectOptions(screen.getByLabelText('Produto'), campos.produto)
}

async function abrirFormulario() {
  const tela = renderApp('/portadores/novo')
  // O seletor só habilita quando os produtos chegam.
  await waitFor(() => expect(screen.getByLabelText('Produto')).toBeEnabled())
  return tela
}

beforeEach(() => authStore.iniciarSessao('jwt', 1800))

describe('cadastro de portador: formulário', () => {
  it('deveListarSomenteProdutosAtivosNoSeletor', async () => {
    const fetchSpy = rotearFetch([rotaProdutos(['Gold Teste', 'Black Teste'])])

    await abrirFormulario()

    const opcoes = within(screen.getByLabelText('Produto')).getAllByRole('option')
    expect(opcoes.map((o) => o.textContent)).toEqual([
      'Selecione um produto',
      'Gold Teste — GOLD',
      'Black Teste — GOLD',
    ])
    expect(String(fetchSpy.mock.calls[0]?.[0])).toContain('status=ATIVO')
  })

  it('deveMostrarOsErrosDeValidacaoSemChamarOBackend', async () => {
    const fetchSpy = rotearFetch([rotaProdutos()])
    const { user } = await abrirFormulario()

    await user.click(screen.getByRole('button', { name: 'Cadastrar' }))

    expect(await screen.findByText('Informe o nome')).toBeInTheDocument()
    expect(screen.getByText('Informe o CPF')).toBeInTheDocument()
    expect(screen.getByText('Informe a data de nascimento')).toBeInTheDocument()
    expect(screen.getByText('Selecione um produto', { selector: 'p' })).toBeInTheDocument()
    expect(chamadasPara(fetchSpy, '/api/v1/portadores', 'POST')).toHaveLength(0)
  })

  it('deveAplicarAMascaraDeCpfEnquantoODigita', async () => {
    rotearFetch([rotaProdutos()])
    const { user } = await abrirFormulario()

    await user.type(screen.getByLabelText('CPF'), '52998224725')

    expect(screen.getByLabelText('CPF')).toHaveValue('529.982.247-25')
  })

  it('deveRejeitarCpfComDigitoVerificadorErrado', async () => {
    rotearFetch([rotaProdutos()])
    const { user } = await abrirFormulario()

    await preencher(user, { nome: 'Victor', cpf: '52998224726', nascimento: '1990-01-31', produto: 'Gold Teste — GOLD' })
    await user.click(screen.getByRole('button', { name: 'Cadastrar' }))

    expect(await screen.findByText('CPF inválido')).toBeInTheDocument()
  })

  it('deveRejeitarMenorDeIdade', async () => {
    rotearFetch([rotaProdutos()])
    const { user } = await abrirFormulario()
    const anoRecente = new Date().getFullYear() - 5

    await preencher(user, { nome: 'Victor', cpf: CPF_VALIDO, nascimento: `${anoRecente}-01-01`, produto: 'Gold Teste — GOLD' })
    await user.click(screen.getByRole('button', { name: 'Cadastrar' }))

    expect(await screen.findByText('É necessário ter 18 anos ou mais', { selector: 'p[role="alert"]' })).toBeInTheDocument()
  })
})

describe('cadastro de portador: envio', () => {
  it('deveCadastrarEIrParaODetalheDoPortador', async () => {
    const fetchSpy = rotearFetch([
      rotaProdutos(),
      { ...ROTA_CADASTRO, resposta: () => respostaJson(201, portadorResponse(), { Location: `/api/v1/portadores/${ID_PORTADOR}` }) },
      rotaCompleto(() => portadorCompleto()),
    ])
    const { user } = await abrirFormulario()

    await preencher(user, { nome: 'Victor Rodrigues', cpf: CPF_VALIDO, nascimento: '1990-01-31', produto: 'Gold Teste — GOLD' })
    await user.click(screen.getByRole('button', { name: 'Cadastrar' }))

    expect(await screen.findByRole('heading', { name: 'Victor Rodrigues' })).toBeInTheDocument()
    const corpo = JSON.parse(String(chamadasPara(fetchSpy, '/api/v1/portadores', 'POST')[0]?.[1]?.body)) as Record<string, string>
    expect(corpo.cpf).toBe('52998224725')
    expect(corpo.produtoId).toBe(paginaDeProdutos().conteudo[0]?.id)
  })

  it('deveDesabilitarOBotaoEnquantoOCadastroEstaEmAndamento', async () => {
    let concluir: (r: Response) => void = () => {}
    rotearFetch([
      rotaProdutos(),
      { ...ROTA_CADASTRO, resposta: () => new Promise<Response>((resolve) => { concluir = resolve }) },
      rotaCompleto(() => portadorCompleto()),
    ])
    const { user } = await abrirFormulario()

    await preencher(user, { nome: 'Victor', cpf: CPF_VALIDO, nascimento: '1990-01-31', produto: 'Gold Teste — GOLD' })
    await user.click(screen.getByRole('button', { name: 'Cadastrar' }))

    expect(await screen.findByRole('button', { name: 'Cadastrando…' })).toBeDisabled()
    concluir(respostaJson(201, portadorResponse()))
    expect(await screen.findByRole('heading', { name: 'Victor Rodrigues' })).toBeInTheDocument()
  })

  it('deveMostrarErroDeCampoDoServidorNoCampoEFocarNele', async () => {
    rotearFetch([
      rotaProdutos(),
      {
        ...ROTA_CADASTRO,
        resposta: () =>
          respostaJson(400, { title: 'Payload inválido', errors: [{ field: 'nome', message: 'não deve estar em branco' }] }),
      },
    ])
    const { user } = await abrirFormulario()

    await preencher(user, { nome: 'Victor', cpf: CPF_VALIDO, nascimento: '1990-01-31', produto: 'Gold Teste — GOLD' })
    await user.click(screen.getByRole('button', { name: 'Cadastrar' }))

    expect(await screen.findByText('não deve estar em branco')).toBeInTheDocument()
    expect(screen.getByLabelText('Nome completo')).toHaveFocus()
    expect(screen.getByLabelText('Nome completo')).toHaveAttribute('aria-invalid', 'true')
  })

  it('deveApontarOCampoCpfQuandoOCpfJaEstaCadastrado', async () => {
    rotearFetch([
      rotaProdutos(),
      { ...ROTA_CADASTRO, resposta: () => respostaJson(409, { title: 'Conflito', detail: 'Já existe um portador cadastrado com este CPF' }) },
    ])
    const { user } = await abrirFormulario()

    await preencher(user, { nome: 'Victor', cpf: CPF_VALIDO, nascimento: '1990-01-31', produto: 'Gold Teste — GOLD' })
    await user.click(screen.getByRole('button', { name: 'Cadastrar' }))

    expect(await screen.findByText('Já existe um portador cadastrado com este CPF')).toBeInTheDocument()
    expect(screen.getByLabelText('CPF')).toHaveFocus()
  })

  it('deveMostrarMensagemGeralQuandoOProdutoNaoEstaAtivo', async () => {
    rotearFetch([
      rotaProdutos(),
      { ...ROTA_CADASTRO, resposta: () => respostaJson(422, { title: 'Regra de negócio violada', detail: 'Produto não está ATIVO', correlationId: 'corr-422' }) },
    ])
    const { user } = await abrirFormulario()

    await preencher(user, { nome: 'Victor', cpf: CPF_VALIDO, nascimento: '1990-01-31', produto: 'Gold Teste — GOLD' })
    await user.click(screen.getByRole('button', { name: 'Cadastrar' }))

    expect(await screen.findByText('Produto não está ATIVO')).toBeInTheDocument()
    expect(screen.getByText('Código de suporte: corr-422')).toBeInTheDocument()
  })

  it('deveExplicarQuandoOProdutoEstaForaDoArNoCadastro', async () => {
    rotearFetch([
      rotaProdutos(),
      { ...ROTA_CADASTRO, resposta: () => respostaJson(503, { title: 'Dependência indisponível' }, { 'Retry-After': '5' }) },
    ])
    const { user } = await abrirFormulario()

    await preencher(user, { nome: 'Victor', cpf: CPF_VALIDO, nascimento: '1990-01-31', produto: 'Gold Teste — GOLD' })
    await user.click(screen.getByRole('button', { name: 'Cadastrar' }))

    expect(await screen.findByText(/temporariamente indisponível/)).toBeInTheDocument()
  })

  it('naoDeveGuardarOCpfEmNenhumArmazenamentoDoBrowser', async () => {
    rotearFetch([
      rotaProdutos(),
      { ...ROTA_CADASTRO, resposta: () => respostaJson(201, portadorResponse()) },
      rotaCompleto(() => portadorCompleto()),
    ])
    const { user } = await abrirFormulario()

    await preencher(user, { nome: 'Victor', cpf: CPF_VALIDO, nascimento: '1990-01-31', produto: 'Gold Teste — GOLD' })
    await user.click(screen.getByRole('button', { name: 'Cadastrar' }))
    await screen.findByRole('heading', { name: 'Victor Rodrigues' })

    expect(localStorage.length).toBe(0)
    expect(sessionStorage.length).toBe(0)
  })
})

describe('cadastro de portador: produtos', () => {
  it('deveAvisarEBloquearOEnvioQuandoNaoHaProdutoAtivo', async () => {
    rotearFetch([{ caminho: '/api/v1/produtos?status=ATIVO', resposta: () => respostaJson(200, paginaDeProdutos([])) }])
    renderApp('/portadores/novo')

    expect(await screen.findByText(/Não há produtos ATIVOS/)).toBeInTheDocument()
    expect(screen.getByRole('button', { name: 'Cadastrar' })).toBeDisabled()
  })

  it('deveOferecerNovaTentativaQuandoAListaDeProdutosFalha', async () => {
    let falhar = true
    const fetchSpy = rotearFetch([
      {
        caminho: '/api/v1/produtos?status=ATIVO',
        resposta: () => (falhar ? respostaJson(422, { detail: 'Falha ao listar' }) : respostaJson(200, paginaDeProdutos())),
      },
    ])
    const { user } = renderApp('/portadores/novo')

    expect(await screen.findByText(/Não foi possível carregar os produtos/)).toBeInTheDocument()
    expect(screen.getByRole('button', { name: 'Cadastrar' })).toBeDisabled()

    falhar = false
    await user.click(screen.getByRole('button', { name: 'Tentar novamente' }))

    await waitFor(() => expect(screen.getByLabelText('Produto')).toBeEnabled())
    expect(chamadasPara(fetchSpy, '/api/v1/produtos')).toHaveLength(2)
  })

  it('deveAvisarQuandoHaMaisProdutosAtivosDoQueOSeletorMostra', async () => {
    rotearFetch([
      {
        caminho: '/api/v1/produtos?status=ATIVO',
        resposta: () => respostaJson(200, { ...paginaDeProdutos(), totalElementos: 250 }),
      },
    ])
    renderApp('/portadores/novo')

    expect(await screen.findByText('Mostrando 1 de 250 produtos ativos.')).toBeInTheDocument()
  })
})

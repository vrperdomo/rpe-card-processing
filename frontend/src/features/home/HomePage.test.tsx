import { screen } from '@testing-library/react'
import { beforeEach, describe, expect, it } from 'vitest'
import { authStore } from '../../lib/auth/authStore'
import { ID_PORTADOR, portadorCompleto, rotaCompleto, rotearFetch } from '../../test/fixtures'
import { renderApp } from '../../test/utils'

beforeEach(() => authStore.iniciarSessao('jwt', 1800))

describe('home', () => {
  it('deveLevarAoFormularioDeCadastro', async () => {
    rotearFetch([{ caminho: '/api/v1/produtos?status=ATIVO', resposta: () => new Response('{}', { status: 500 }) }])
    const { user } = renderApp('/')

    await user.click(screen.getByRole('link', { name: 'Cadastrar portador' }))

    expect(await screen.findByRole('heading', { name: 'Cadastrar portador' })).toBeInTheDocument()
  })

  it('deveAbrirODetalheDoPortadorPeloIdentificador', async () => {
    rotearFetch([rotaCompleto(() => portadorCompleto({ emissao: 'PENDENTE' }))])
    const { user } = renderApp('/')

    await user.type(screen.getByLabelText('Identificador do portador'), `  ${ID_PORTADOR}  `)
    await user.click(screen.getByRole('button', { name: 'Consultar' }))

    expect(await screen.findByRole('heading', { name: 'Victor Rodrigues' })).toBeInTheDocument()
  })

  it('deveValidarOIdentificadorAntesDeNavegar', async () => {
    const fetchSpy = rotearFetch([])
    const { user } = renderApp('/')

    await user.click(screen.getByRole('button', { name: 'Consultar' }))
    expect(await screen.findByText('Informe o identificador')).toBeInTheDocument()

    await user.type(screen.getByLabelText('Identificador do portador'), 'abc')
    await user.click(screen.getByRole('button', { name: 'Consultar' }))
    expect(await screen.findByText('Identificador inválido')).toBeInTheDocument()
    expect(fetchSpy).not.toHaveBeenCalled()
  })
})

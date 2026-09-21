import { ApiError } from '../../lib/api/apiError'
import { mensagemDeErro } from '../../lib/api/mensagemDeErro'
import type { CadastroPortadorForm } from './portadoresApi'

type Campo = keyof CadastroPortadorForm

export interface ErroNoCampo {
  campo: Campo
  mensagem: string
}

const CAMPOS: readonly string[] = ['nome', 'cpf', 'dataNascimento', 'produtoId'] satisfies Campo[]

function ehCampo(nome: string): nome is Campo {
  return CAMPOS.includes(nome)
}

// Quais campos do formulário o erro do servidor explica. Erros de validação (400 com errors[])
// apontam o campo pelo nome; o 409 do cadastro é o CPF já cadastrado, então também cai no CPF.
// Lista vazia = erro que não é de campo, e a tela mostra a mensagem geral.
export function camposAfetados(erro: unknown): ErroNoCampo[] {
  if (!(erro instanceof ApiError)) return []
  const afetados = erro.errosDeCampo.flatMap((e) =>
    ehCampo(e.field) ? [{ campo: e.field, mensagem: e.message }] : [],
  )
  if (erro.status === 409 && !afetados.some((a) => a.campo === 'cpf')) {
    afetados.push({ campo: 'cpf', mensagem: mensagemDeErro(erro) })
  }
  return afetados
}

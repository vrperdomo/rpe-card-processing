import { z } from 'zod'
import { apiFetch } from '../../lib/api/client'
import { cpfValido, somenteDigitos } from '../../lib/validators/cpf'
import { estaNoPassado, hojeCivil, idadeEmAnos, lerDataIso } from '../../lib/validators/data'

export const IDADE_MINIMA_ANOS = 18

// Espelha as regras do backend (CadastrarPortadorRequest + Portador) só para dar retorno imediato;
// a validação que vale é a do servidor, cujos erros de campo também são exibidos.
export const cadastroPortadorSchema = z.object({
  nome: z
    .string()
    .trim()
    .min(1, 'Informe o nome')
    .max(150, 'O nome deve ter no máximo 150 caracteres'),
  cpf: z.string().min(1, 'Informe o CPF').refine(cpfValido, 'CPF inválido'),
  dataNascimento: z
    .string()
    .min(1, 'Informe a data de nascimento')
    .superRefine((valor, ctx) => {
      if (valor === '') return
      const data = lerDataIso(valor)
      const hoje = hojeCivil()
      if (!data) {
        ctx.addIssue({ code: 'custom', message: 'Data inválida' })
      } else if (!estaNoPassado(data, hoje)) {
        ctx.addIssue({ code: 'custom', message: 'A data de nascimento deve estar no passado' })
      } else if (idadeEmAnos(data, hoje) < IDADE_MINIMA_ANOS) {
        ctx.addIssue({ code: 'custom', message: `É necessário ter ${IDADE_MINIMA_ANOS} anos ou mais` })
      }
    }),
  produtoId: z.guid('Selecione um produto'),
})

export type CadastroPortadorForm = z.infer<typeof cadastroPortadorSchema>

const portadorSchema = z.object({
  id: z.guid(),
  nome: z.string(),
  // Sempre mascarado pela API (***.456.789-**); a UI só exibe, nunca reconstrói.
  cpf: z.string(),
  dataNascimento: z.string(),
  produtoId: z.guid(),
  status: z.string(),
})

const cartaoSchema = z.object({
  id: z.guid(),
  panMascarado: z.string(),
  validade: z.string(),
  status: z.string(),
})

const produtoResumoSchema = z.object({
  id: z.guid(),
  nome: z.string(),
  categoria: z.string(),
  status: z.string(),
})

// Valor novo vindo do backend cai em DESCONHECIDA em vez de quebrar a tela.
export const statusEmissaoSchema = z.enum(['CONCLUIDA', 'PENDENTE', 'DESCONHECIDA']).catch('DESCONHECIDA')

export type StatusEmissao = z.infer<typeof statusEmissaoSchema>

const portadorCompletoSchema = z.object({
  portador: portadorSchema,
  // null enquanto o cartão não foi emitido ou o Cartão está fora do ar.
  cartao: cartaoSchema.nullish().transform((valor) => valor ?? null),
  // null quando o Produto está fora do ar (resposta degradada, com avisos).
  produto: produtoResumoSchema.nullish().transform((valor) => valor ?? null),
  emissao: statusEmissaoSchema,
  avisos: z
    .array(z.string())
    .nullish()
    .transform((valor) => valor ?? []),
})

export type Portador = z.infer<typeof portadorSchema>
export type PortadorCompleto = z.infer<typeof portadorCompletoSchema>

// O backend aceita o CPF com ou sem máscara; a UI manda só os dígitos.
export async function cadastrarPortador(form: CadastroPortadorForm): Promise<Portador> {
  const resposta = await apiFetch<unknown>('/api/v1/portadores', {
    metodo: 'POST',
    corpo: {
      nome: form.nome.trim(),
      cpf: somenteDigitos(form.cpf),
      dataNascimento: form.dataNascimento,
      produtoId: form.produtoId,
    },
  })
  return portadorSchema.parse(resposta)
}

export async function buscarPortadorCompleto(
  id: string,
  signal?: AbortSignal,
): Promise<PortadorCompleto> {
  const resposta = await apiFetch<unknown>(
    `/api/v1/portadores/${encodeURIComponent(id)}/completo`,
    signal ? { signal } : {},
  )
  return portadorCompletoSchema.parse(resposta)
}

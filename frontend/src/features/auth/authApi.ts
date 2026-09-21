import { z } from 'zod'
import { apiFetch } from '../../lib/api/client'

export const loginSchema = z.object({
  username: z.string().trim().min(1, 'Informe o usuário'),
  // Sem trim: espaços podem fazer parte da senha.
  password: z.string().min(1, 'Informe a senha'),
})

export type Credenciais = z.infer<typeof loginSchema>

// Valida o contrato da resposta em runtime: se o backend mudar o formato, o erro aparece aqui, não
// como um token "undefined" silencioso.
const respostaLoginSchema = z.object({
  accessToken: z.string().min(1),
  tokenType: z.string(),
  expiresIn: z.number().positive(),
})

export type RespostaLogin = z.infer<typeof respostaLoginSchema>

export async function login(credenciais: Credenciais): Promise<RespostaLogin> {
  const resposta = await apiFetch<unknown>('/api/v1/auth/login', {
    metodo: 'POST',
    corpo: credenciais,
    publica: true,
  })
  return respostaLoginSchema.parse(resposta)
}

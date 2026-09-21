import { useMutation } from '@tanstack/react-query'
import { authStore } from '../../lib/auth/authStore'
import { login } from './authApi'

export function useLogin() {
  return useMutation({
    mutationFn: login,
    onSuccess: (resposta) => authStore.iniciarSessao(resposta.accessToken, resposta.expiresIn),
  })
}

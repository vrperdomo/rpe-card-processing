import { zodResolver } from '@hookform/resolvers/zod'
import { useForm } from 'react-hook-form'
import { Navigate, useLocation } from 'react-router-dom'
import { codigoDeSuporte, mensagemDeErro } from '../../lib/api/mensagemDeErro'
import type { MotivoSaida } from '../../lib/auth/authStore'
import { useAuth } from '../../lib/auth/useAuth'
import { loginSchema, type Credenciais } from './authApi'
import { destinoAposLogin } from './destinoAposLogin'
import { useLogin } from './useLogin'

const AVISOS_DE_SAIDA: Partial<Record<MotivoSaida, string>> = {
  expirada: 'Sua sessão expirou. Entre novamente para continuar.',
  'nao-autorizado': 'Sua sessão não é mais válida. Entre novamente para continuar.',
}

const classeCampo =
  'w-full rounded-md border border-slate-300 bg-white px-3 py-2 text-slate-900 shadow-sm ' +
  'focus:border-indigo-600 focus:outline-none focus:ring-2 focus:ring-indigo-600/30 ' +
  'aria-[invalid=true]:border-red-600 dark:border-slate-600 dark:bg-slate-900 dark:text-slate-100'

export default function LoginPage() {
  const { autenticado, motivoSaida } = useAuth()
  const location = useLocation()
  const login = useLogin()
  const {
    register,
    handleSubmit,
    resetField,
    setFocus,
    formState: { errors },
  } = useForm<Credenciais>({
    resolver: zodResolver(loginSchema),
    defaultValues: { username: '', password: '' },
  })

  if (autenticado) return <Navigate to={destinoAposLogin(location.state)} replace />

  const aviso = motivoSaida === null ? undefined : AVISOS_DE_SAIDA[motivoSaida]
  const suporte = codigoDeSuporte(login.error)

  // Depois de uma falha a senha digitada não fica no campo, e o foco volta para ela.
  const aoEnviar = (dados: Credenciais) =>
    login.mutate(dados, {
      onError: () => {
        resetField('password')
        setFocus('password')
      },
    })

  return (
    <main className="flex min-h-screen items-center justify-center bg-slate-50 px-4 text-slate-900 dark:bg-slate-900 dark:text-slate-100">
      <div className="w-full max-w-sm rounded-xl border border-slate-200 bg-white p-8 shadow-sm dark:border-slate-700 dark:bg-slate-800">
        <h1 className="text-xl font-semibold text-slate-900 dark:text-slate-100">
          RPE Card Processing
        </h1>
        <p className="mt-1 text-sm text-slate-600 dark:text-slate-300">
          Entre para cadastrar portadores e acompanhar a emissão de cartões.
        </p>

        {aviso && (
          <p
            role="status"
            className="mt-4 rounded-md bg-amber-50 px-3 py-2 text-sm text-amber-800 dark:bg-amber-900/30 dark:text-amber-200"
          >
            {aviso}
          </p>
        )}

        <form className="mt-6 space-y-4" noValidate onSubmit={(e) => void handleSubmit(aoEnviar)(e)}>
          <div>
            <label htmlFor="username" className="mb-1 block text-sm font-medium">
              Usuário
            </label>
            <input
              id="username"
              type="text"
              autoComplete="username"
              autoFocus
              aria-invalid={errors.username ? 'true' : 'false'}
              aria-describedby={errors.username ? 'username-erro' : undefined}
              className={classeCampo}
              {...register('username')}
            />
            {errors.username && (
              <p id="username-erro" role="alert" className="mt-1 text-sm text-red-700 dark:text-red-400">
                {errors.username.message}
              </p>
            )}
          </div>

          <div>
            <label htmlFor="password" className="mb-1 block text-sm font-medium">
              Senha
            </label>
            <input
              id="password"
              type="password"
              autoComplete="current-password"
              aria-invalid={errors.password ? 'true' : 'false'}
              aria-describedby={errors.password ? 'password-erro' : undefined}
              className={classeCampo}
              {...register('password')}
            />
            {errors.password && (
              <p id="password-erro" role="alert" className="mt-1 text-sm text-red-700 dark:text-red-400">
                {errors.password.message}
              </p>
            )}
          </div>

          {login.isError && (
            <div role="alert" className="rounded-md bg-red-50 px-3 py-2 text-sm text-red-800 dark:bg-red-900/30 dark:text-red-200">
              <p>{mensagemDeErro(login.error)}</p>
              {suporte && <p className="mt-1 text-xs opacity-80">Código de suporte: {suporte}</p>}
            </div>
          )}

          <button
            type="submit"
            disabled={login.isPending}
            className="w-full rounded-md bg-indigo-600 px-4 py-2 font-medium text-white hover:bg-indigo-700 focus:outline-none focus:ring-2 focus:ring-indigo-600/50 disabled:cursor-not-allowed disabled:opacity-60"
          >
            {login.isPending ? 'Entrando…' : 'Entrar'}
          </button>
        </form>
      </div>
    </main>
  )
}

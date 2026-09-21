import { useQueryClient } from '@tanstack/react-query'
import { Outlet } from 'react-router-dom'
import { authStore } from '../lib/auth/authStore'

export default function AppLayout() {
  const queryClient = useQueryClient()

  // Além de descartar o token, esvazia o cache de dados: o que foi carregado nesta sessão não
  // deve aparecer para quem entrar em seguida no mesmo navegador.
  function sair() {
    authStore.encerrar('manual')
    queryClient.clear()
  }

  return (
    <div className="min-h-screen bg-slate-50 text-slate-900 dark:bg-slate-900 dark:text-slate-100">
      <header className="flex items-center justify-between border-b border-slate-200 bg-white px-6 py-4 dark:border-slate-700 dark:bg-slate-800">
        <span className="text-lg font-semibold">RPE Card Processing</span>
        <button
          type="button"
          onClick={sair}
          className="rounded-md border border-slate-300 px-3 py-1.5 text-sm font-medium hover:bg-slate-100 focus:outline-none focus:ring-2 focus:ring-indigo-600/50 dark:border-slate-600 dark:hover:bg-slate-700"
        >
          Sair
        </button>
      </header>
      <main className="mx-auto max-w-3xl px-6 py-10">
        <Outlet />
      </main>
    </div>
  )
}

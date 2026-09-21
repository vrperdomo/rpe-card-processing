import { useSyncExternalStore } from 'react'
import { authStore, type EstadoAuth } from './authStore'

export function useAuth(): EstadoAuth & { autenticado: boolean } {
  const estado = useSyncExternalStore(authStore.assinar, authStore.obterEstado)
  return { ...estado, autenticado: estado.token !== null }
}

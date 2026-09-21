import { Navigate, Outlet, useLocation } from 'react-router-dom'
import { useAuth } from '../../lib/auth/useAuth'

// Guarda das rotas privadas: sem token em memória, vai para /login e lembra de onde veio para
// voltar depois do login.
export default function RequireAuth() {
  const { autenticado } = useAuth()
  const location = useLocation()

  if (!autenticado) {
    const origem = location.pathname + location.search
    return <Navigate to="/login" replace state={{ from: origem }} />
  }
  return <Outlet />
}

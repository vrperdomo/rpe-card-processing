import { Navigate, Route, Routes } from 'react-router-dom'
import AppLayout from './components/AppLayout'
import LoginPage from './features/auth/LoginPage'
import RequireAuth from './features/auth/RequireAuth'
import HomePage from './features/home/HomePage'

export default function App() {
  return (
    <Routes>
      <Route path="/login" element={<LoginPage />} />
      <Route element={<RequireAuth />}>
        <Route element={<AppLayout />}>
          <Route index element={<HomePage />} />
        </Route>
      </Route>
      {/* Rota desconhecida cai na home, e a guarda decide se pede login. */}
      <Route path="*" element={<Navigate to="/" replace />} />
    </Routes>
  )
}

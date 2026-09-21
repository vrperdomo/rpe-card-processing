import '@testing-library/jest-dom/vitest'
import { cleanup } from '@testing-library/react'
import { afterEach, vi } from 'vitest'
import { authStore } from '../lib/auth/authStore'

// Sem globals do Vitest, o auto-cleanup do Testing Library não é registrado sozinho. O resto
// devolve o ambiente ao zero para que um teste nunca dependa do que o anterior deixou.
afterEach(() => {
  cleanup()
  vi.restoreAllMocks()
  vi.useRealTimers()
  authStore.reiniciar()
  localStorage.clear()
  sessionStorage.clear()
})

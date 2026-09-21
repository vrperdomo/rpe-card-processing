import { QueryClientProvider } from '@tanstack/react-query'
import { StrictMode } from 'react'
import { createRoot } from 'react-dom/client'
import { BrowserRouter } from 'react-router-dom'
import App from './App'
import './index.css'
import { criarQueryClient } from './lib/queryClient'

const raiz = document.getElementById('root')
if (!raiz) {
  throw new Error('Elemento #root não encontrado em index.html')
}

const queryClient = criarQueryClient()

createRoot(raiz).render(
  <StrictMode>
    <QueryClientProvider client={queryClient}>
      <BrowserRouter>
        <App />
      </BrowserRouter>
    </QueryClientProvider>
  </StrictMode>,
)

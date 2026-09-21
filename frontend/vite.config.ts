import tailwindcss from '@tailwindcss/vite'
import react from '@vitejs/plugin-react'
import { defineConfig } from 'vitest/config'

// Em dev, o Vite faz o papel do Nginx de produção (frontend/nginx.conf): o browser só fala com
// uma origem e as rotas /api/v1/* são encaminhadas ao serviço dono de cada recurso. Assim o
// código usa sempre caminhos relativos e nenhum serviço precisa de CORS.
const servicos = {
  portador: process.env.VITE_PROXY_PORTADOR ?? 'http://localhost:8082',
  produto: process.env.VITE_PROXY_PRODUTO ?? 'http://localhost:8081',
  cartao: process.env.VITE_PROXY_CARTAO ?? 'http://localhost:8083',
}

export default defineConfig({
  plugins: [react(), tailwindcss()],
  server: {
    port: 5173,
    proxy: {
      '/api/v1/auth': servicos.portador,
      '/api/v1/portadores': servicos.portador,
      '/api/v1/produtos': servicos.produto,
      '/api/v1/cartoes': servicos.cartao,
    },
  },
  test: {
    environment: 'jsdom',
    setupFiles: ['./src/test/setup.ts'],
    css: false,
  },
})

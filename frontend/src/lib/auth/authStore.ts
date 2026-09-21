// Armazenamento do JWT SOMENTE em memória (ADR-008): nunca localStorage, sessionStorage nem cookie.
// Recarregar a página encerra a sessão, e esse é o custo aceito por não deixar o token ao alcance
// de scripts injetados nem persistido no disco.

export type MotivoSaida = 'manual' | 'expirada' | 'nao-autorizado'

export interface EstadoAuth {
  readonly token: string | null
  // Por que a sessão terminou; a tela de login usa isso para explicar (exceto 'manual').
  readonly motivoSaida: MotivoSaida | null
}

const ESTADO_INICIAL: EstadoAuth = { token: null, motivoSaida: null }
// setTimeout estoura acima de 2^31-1 ms (~24,8 dias); o token real dura minutos.
const MAX_TIMEOUT_MS = 2_147_483_647

let estado: EstadoAuth = ESTADO_INICIAL
let temporizadorExpiracao: ReturnType<typeof setTimeout> | undefined
const ouvintes = new Set<() => void>()

function publicar(novo: EstadoAuth) {
  estado = novo
  ouvintes.forEach((ouvinte) => ouvinte())
}

function cancelarTemporizador() {
  clearTimeout(temporizadorExpiracao)
  temporizadorExpiracao = undefined
}

export const authStore = {
  // Referências estáveis para useSyncExternalStore: obterEstado só devolve um objeto novo quando
  // o estado muda de fato.
  obterEstado: (): EstadoAuth => estado,

  obterToken: (): string | null => estado.token,

  assinar(ouvinte: () => void): () => void {
    ouvintes.add(ouvinte)
    return () => {
      ouvintes.delete(ouvinte)
    }
  },

  iniciarSessao(token: string, expiraEmSegundos: number) {
    cancelarTemporizador()
    const ms = Math.min(Math.max(expiraEmSegundos, 0) * 1000, MAX_TIMEOUT_MS)
    temporizadorExpiracao = setTimeout(() => authStore.encerrar('expirada'), ms)
    publicar({ token, motivoSaida: null })
  },

  // Sem sessão ativa não há o que encerrar: evita que várias respostas 401 em paralelo
  // sobrescrevam o motivo da primeira.
  encerrar(motivo: MotivoSaida) {
    if (estado.token === null) return
    cancelarTemporizador()
    publicar({ token: null, motivoSaida: motivo })
  },

  // Volta ao estado de "nunca autenticou" (usado nos testes).
  reiniciar() {
    cancelarTemporizador()
    publicar(ESTADO_INICIAL)
  },
}

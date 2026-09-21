import { useMutation, useQuery } from '@tanstack/react-query'
import { useEffect, useRef, useState } from 'react'
import { buscarPortadorCompleto, cadastrarPortador } from './portadoresApi'
import { intervaloDePolling, POLLING } from './polling'

export function useCadastrarPortador() {
  return useMutation({ mutationFn: cadastrarPortador })
}

// Consulta o portador agregado e repete enquanto a emissão não concluir, até POLLING.limiteMs
// contados da montagem. O componente que usa este hook deve ter key={id}: trocar de portador
// remonta a tela e reinicia a contagem.
export function usePortadorCompleto(id: string | undefined) {
  const [pollingEsgotado, setPollingEsgotado] = useState(false)
  // Lido pelo refetchInterval sem recriar a query a cada mudança.
  const esgotadoRef = useRef(false)

  useEffect(() => {
    const temporizador = setTimeout(() => {
      esgotadoRef.current = true
      setPollingEsgotado(true)
    }, POLLING.limiteMs)
    return () => clearTimeout(temporizador)
  }, [])

  const consulta = useQuery({
    queryKey: ['portadores', id, 'completo'],
    queryFn: ({ signal }) => buscarPortadorCompleto(id as string, signal),
    enabled: id !== undefined,
    refetchInterval: (query) =>
      esgotadoRef.current ? false : intervaloDePolling(query.state.data?.emissao),
  })

  const emissaoIncompleta = consulta.data !== undefined && consulta.data.emissao !== 'CONCLUIDA'
  return { ...consulta, pollingEsgotado: pollingEsgotado && emissaoIncompleta }
}

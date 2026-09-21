import type { ReactNode } from 'react'

// Atributos que ligam o controle ao rótulo e à mensagem de erro para leitores de tela.
export function propsAcessiveis(id: string, erro: string | undefined) {
  return {
    id,
    'aria-invalid': erro ? ('true' as const) : ('false' as const),
    'aria-describedby': erro ? `${id}-erro` : undefined,
  }
}

interface CampoProps {
  id: string
  rotulo: string
  erro?: string | undefined
  ajuda?: string
  children: ReactNode
}

export default function Campo({ id, rotulo, erro, ajuda, children }: CampoProps) {
  return (
    <div>
      <label htmlFor={id} className="mb-1 block text-sm font-medium">
        {rotulo}
      </label>
      {children}
      {ajuda && !erro && <p className="mt-1 text-xs text-slate-500 dark:text-slate-400">{ajuda}</p>}
      {erro && (
        <p id={`${id}-erro`} role="alert" className="mt-1 text-sm text-red-700 dark:text-red-400">
          {erro}
        </p>
      )}
    </div>
  )
}

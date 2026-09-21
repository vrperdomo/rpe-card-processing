import type { StatusEmissao } from './portadoresApi'

interface Apresentacao {
  titulo: string
  descricao: string
  classe: string
}

function apresentar(emissao: StatusEmissao, pollingEsgotado: boolean): Apresentacao {
  switch (emissao) {
    case 'CONCLUIDA':
      return {
        titulo: 'Cartão emitido',
        descricao: 'A emissão foi concluída.',
        classe: 'bg-green-50 text-green-900 dark:bg-green-900/30 dark:text-green-200',
      }
    case 'PENDENTE':
      return {
        titulo: 'Emissão em andamento',
        descricao: pollingEsgotado
          ? 'A emissão está demorando mais que o esperado. Ela continua sendo processada; use "Atualizar" para conferir.'
          : 'O cartão está sendo emitido. Esta tela atualiza sozinha.',
        classe: 'bg-amber-50 text-amber-900 dark:bg-amber-900/30 dark:text-amber-200',
      }
    case 'DESCONHECIDA':
      return {
        titulo: 'Situação da emissão indisponível',
        descricao: pollingEsgotado
          ? 'O serviço de cartões continua sem responder. Use "Atualizar" mais tarde.'
          : 'Não foi possível consultar o serviço de cartões agora. Vamos tentar novamente.',
        classe: 'bg-slate-100 text-slate-900 dark:bg-slate-700 dark:text-slate-100',
      }
  }
}

// aria-live: quando o polling muda o estado, o leitor de tela anuncia sem o usuário procurar.
export default function EmissaoStatus({
  emissao,
  pollingEsgotado,
}: {
  emissao: StatusEmissao
  pollingEsgotado: boolean
}) {
  const { titulo, descricao, classe } = apresentar(emissao, pollingEsgotado)
  return (
    <div role="status" aria-live="polite" className={`rounded-md px-4 py-3 ${classe}`}>
      <p className="font-semibold">{titulo}</p>
      <p className="mt-0.5 text-sm">{descricao}</p>
    </div>
  )
}

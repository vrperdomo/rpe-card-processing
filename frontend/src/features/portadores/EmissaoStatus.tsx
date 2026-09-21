import { formatarInstante } from '../../lib/formato/dataHora'
import type { FalhaEmissao, StatusEmissao } from './portadoresApi'

interface Apresentacao {
  titulo: string
  descricao: string
  // Linhas extras (ex.: motivo da falha), uma por item.
  detalhes?: string[]
  classe: string
}

function detalhesDaFalha(falha: FalhaEmissao | null): string[] {
  if (falha === null) return []
  return [`Motivo: ${falha.motivo}`, `Ocorrido em ${formatarInstante(falha.ocorridaEm)}`]
}

function apresentar(
  emissao: StatusEmissao,
  pollingEsgotado: boolean,
  falha: FalhaEmissao | null,
): Apresentacao {
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
    case 'FALHOU':
      return {
        titulo: 'Não foi possível emitir o cartão',
        descricao: 'A emissão falhou e não será tentada de novo automaticamente.',
        detalhes: detalhesDaFalha(falha),
        classe: 'bg-red-50 text-red-900 dark:bg-red-900/30 dark:text-red-200',
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
// A falha é anunciada com mais urgência (alert) que os demais estados (status).
export default function EmissaoStatus({
  emissao,
  pollingEsgotado,
  falha,
}: {
  emissao: StatusEmissao
  pollingEsgotado: boolean
  falha: FalhaEmissao | null
}) {
  const { titulo, descricao, detalhes = [], classe } = apresentar(emissao, pollingEsgotado, falha)
  return (
    <div
      role={emissao === 'FALHOU' ? 'alert' : 'status'}
      aria-live={emissao === 'FALHOU' ? 'assertive' : 'polite'}
      className={`rounded-md px-4 py-3 ${classe}`}
    >
      <p className="font-semibold">{titulo}</p>
      <p className="mt-0.5 text-sm">{descricao}</p>
      {detalhes.map((linha) => (
        <p key={linha} className="mt-0.5 text-sm">
          {linha}
        </p>
      ))}
    </div>
  )
}

import type { ReactNode } from 'react'
import { Link, useParams } from 'react-router-dom'
import { z } from 'zod'
import { classeBotaoSecundario, classeCartao } from '../../components/estilos'
import { ApiError } from '../../lib/api/apiError'
import { codigoDeSuporte, mensagemDeErro } from '../../lib/api/mensagemDeErro'
import { formatarDataIso } from '../../lib/validators/data'
import EmissaoStatus from './EmissaoStatus'
import { usePortadorCompleto } from './usePortadores'

const idSchema = z.guid()

function rotulo(valor: string): string {
  return valor.charAt(0).toUpperCase() + valor.slice(1).toLowerCase()
}

function Dado({ nome, children }: { nome: string; children: ReactNode }) {
  return (
    <div>
      <dt className="text-xs uppercase tracking-wide text-slate-500 dark:text-slate-400">{nome}</dt>
      <dd className="mt-0.5">{children}</dd>
    </div>
  )
}

function Bloco({ titulo, children }: { titulo: string; children: ReactNode }) {
  return (
    <section className={classeCartao} aria-label={titulo}>
      <h2 className="text-sm font-semibold uppercase tracking-wide text-slate-500 dark:text-slate-400">
        {titulo}
      </h2>
      <dl className="mt-3 grid gap-4 sm:grid-cols-2">{children}</dl>
    </section>
  )
}

// key={id}: trocar de portador remonta o detalhe e reinicia o limite do polling.
export default function PortadorDetalhePage() {
  const { id } = useParams()
  return <PortadorDetalhe key={id} id={id} />
}

function PortadorDetalhe({ id }: { id: string | undefined }) {
  const idValido = id !== undefined && idSchema.safeParse(id).success
  const consulta = usePortadorCompleto(idValido ? id : undefined)

  if (!idValido) {
    return <Mensagem titulo="Identificador inválido">O identificador informado não é válido.</Mensagem>
  }
  if (consulta.isPending) {
    return (
      <p role="status" className="text-slate-600 dark:text-slate-300">
        Carregando portador…
      </p>
    )
  }
  if (consulta.data === undefined) {
    const naoEncontrado = consulta.error instanceof ApiError && consulta.error.status === 404
    return (
      <Mensagem
        titulo={naoEncontrado ? 'Portador não encontrado' : 'Não foi possível carregar o portador'}
        suporte={codigoDeSuporte(consulta.error)}
        acao={
          naoEncontrado ? undefined : (
            <button type="button" className={classeBotaoSecundario} onClick={() => void consulta.refetch()}>
              Tentar novamente
            </button>
          )
        }
      >
        {mensagemDeErro(consulta.error)}
      </Mensagem>
    )
  }

  const { portador, cartao, produto, emissao, avisos } = consulta.data
  return (
    <section>
      <Link to="/" className="text-sm text-indigo-700 hover:underline dark:text-indigo-300">
        ← Início
      </Link>
      <div className="mt-3 flex flex-wrap items-center justify-between gap-3">
        <h1 className="text-2xl font-semibold">{portador.nome}</h1>
        <button
          type="button"
          className={classeBotaoSecundario}
          disabled={consulta.isFetching}
          onClick={() => void consulta.refetch()}
        >
          {consulta.isFetching ? 'Atualizando…' : 'Atualizar'}
        </button>
      </div>

      <div className="mt-4 space-y-4">
        <EmissaoStatus emissao={emissao} pollingEsgotado={consulta.pollingEsgotado} />

        {consulta.isError && (
          <p role="alert" className="rounded-md bg-red-50 px-3 py-2 text-sm text-red-800 dark:bg-red-900/30 dark:text-red-200">
            Não foi possível atualizar agora. Mostrando os últimos dados conhecidos.
          </p>
        )}

        {avisos.length > 0 && (
          <ul
            role="status"
            className="list-disc space-y-1 rounded-md bg-amber-50 py-2 pl-7 pr-3 text-sm text-amber-900 dark:bg-amber-900/30 dark:text-amber-200"
          >
            {avisos.map((aviso) => (
              <li key={aviso}>{aviso}</li>
            ))}
          </ul>
        )}

        <Bloco titulo="Portador">
          <Dado nome="CPF">{portador.cpf}</Dado>
          <Dado nome="Data de nascimento">{formatarDataIso(portador.dataNascimento)}</Dado>
          <Dado nome="Situação">{rotulo(portador.status)}</Dado>
        </Bloco>

        <Bloco titulo="Produto">
          {produto ? (
            <>
              <Dado nome="Nome">{produto.nome}</Dado>
              <Dado nome="Categoria">{rotulo(produto.categoria)}</Dado>
              <Dado nome="Situação">{rotulo(produto.status)}</Dado>
            </>
          ) : (
            <Dado nome="Produto">Indisponível no momento</Dado>
          )}
        </Bloco>

        <Bloco titulo="Cartão">
          {cartao ? (
            <>
              <Dado nome="Número">
                <span className="font-mono">{cartao.panMascarado}</span>
              </Dado>
              <Dado nome="Validade">{cartao.validade}</Dado>
              <Dado nome="Situação">{rotulo(cartao.status)}</Dado>
            </>
          ) : (
            <Dado nome="Cartão">Ainda não emitido</Dado>
          )}
        </Bloco>
      </div>
    </section>
  )
}

function Mensagem({
  titulo,
  children,
  suporte,
  acao,
}: {
  titulo: string
  children: ReactNode
  suporte?: string | undefined
  acao?: ReactNode
}) {
  return (
    <section>
      <Link to="/" className="text-sm text-indigo-700 hover:underline dark:text-indigo-300">
        ← Início
      </Link>
      <div role="alert" className={`mt-4 ${classeCartao}`}>
        <h1 className="text-xl font-semibold">{titulo}</h1>
        <p className="mt-2 text-slate-600 dark:text-slate-300">{children}</p>
        {suporte && <p className="mt-2 text-xs text-slate-500">Código de suporte: {suporte}</p>}
        {acao && <div className="mt-4">{acao}</div>}
      </div>
    </section>
  )
}

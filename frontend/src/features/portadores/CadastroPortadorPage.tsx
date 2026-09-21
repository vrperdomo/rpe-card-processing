import { zodResolver } from '@hookform/resolvers/zod'
import { useForm } from 'react-hook-form'
import { Link, useNavigate } from 'react-router-dom'
import Campo, { propsAcessiveis } from '../../components/Campo'
import { classeBotaoPrimario, classeCampo, classeCartao } from '../../components/estilos'
import { codigoDeSuporte, mensagemDeErro } from '../../lib/api/mensagemDeErro'
import { formatarCpf } from '../../lib/validators/cpf'
import { hojeCivil } from '../../lib/validators/data'
import { useProdutosAtivos } from '../produtos/produtosApi'
import { camposAfetados } from './errosDoServidor'
import { cadastroPortadorSchema, type CadastroPortadorForm } from './portadoresApi'
import { useCadastrarPortador } from './usePortadores'

function dataDeHojeIso(): string {
  const { ano, mes, dia } = hojeCivil()
  return `${ano}-${String(mes).padStart(2, '0')}-${String(dia).padStart(2, '0')}`
}

export default function CadastroPortadorPage() {
  const navigate = useNavigate()
  const produtos = useProdutosAtivos()
  const cadastro = useCadastrarPortador()
  const {
    register,
    handleSubmit,
    setError,
    setFocus,
    formState: { errors },
  } = useForm<CadastroPortadorForm>({
    resolver: zodResolver(cadastroPortadorSchema),
    defaultValues: { nome: '', cpf: '', dataNascimento: '', produtoId: '' },
  })

  const catalogo = produtos.data?.conteudo ?? []
  const semProdutos = produtos.isSuccess && catalogo.length === 0
  const suporte = codigoDeSuporte(cadastro.error)
  const temErroDeCampo = camposAfetados(cadastro.error).length > 0

  const aoEnviar = (dados: CadastroPortadorForm) =>
    cadastro.mutate(dados, {
      onSuccess: (portador) => void navigate(`/portadores/${portador.id}`),
      onError: (erro) => {
        const afetados = camposAfetados(erro)
        afetados.forEach(({ campo, mensagem }) => setError(campo, { type: 'server', message: mensagem }))
        if (afetados[0]) setFocus(afetados[0].campo)
      },
    })

  return (
    <section>
      <Link to="/" className="text-sm text-indigo-700 hover:underline dark:text-indigo-300">
        ← Início
      </Link>
      <h1 className="mt-3 text-2xl font-semibold">Cadastrar portador</h1>
      <p className="mt-1 text-slate-600 dark:text-slate-300">
        Ao cadastrar, a emissão do cartão é disparada automaticamente.
      </p>

      <form
        className={`mt-6 space-y-4 ${classeCartao}`}
        noValidate
        onSubmit={(e) => void handleSubmit(aoEnviar)(e)}
      >
        <Campo id="nome" rotulo="Nome completo" erro={errors.nome?.message}>
          <input
            type="text"
            autoComplete="name"
            className={classeCampo}
            {...propsAcessiveis('nome', errors.nome?.message)}
            {...register('nome')}
          />
        </Campo>

        <Campo id="cpf" rotulo="CPF" erro={errors.cpf?.message}>
          <input
            type="text"
            inputMode="numeric"
            autoComplete="off"
            placeholder="000.000.000-00"
            className={classeCampo}
            {...propsAcessiveis('cpf', errors.cpf?.message)}
            {...register('cpf', {
              onChange: (e: React.ChangeEvent<HTMLInputElement>) => {
                e.target.value = formatarCpf(e.target.value)
              },
            })}
          />
        </Campo>

        <Campo
          id="dataNascimento"
          rotulo="Data de nascimento"
          erro={errors.dataNascimento?.message}
          ajuda="É necessário ter 18 anos ou mais."
        >
          <input
            type="date"
            autoComplete="bday"
            max={dataDeHojeIso()}
            className={classeCampo}
            {...propsAcessiveis('dataNascimento', errors.dataNascimento?.message)}
            {...register('dataNascimento')}
          />
        </Campo>

        <Campo id="produtoId" rotulo="Produto" erro={errors.produtoId?.message}>
          <select
            disabled={!produtos.isSuccess}
            className={classeCampo}
            {...propsAcessiveis('produtoId', errors.produtoId?.message)}
            {...register('produtoId')}
          >
            <option value="">
              {produtos.isPending ? 'Carregando produtos…' : 'Selecione um produto'}
            </option>
            {catalogo.map((produto) => (
              <option key={produto.id} value={produto.id}>
                {produto.nome} — {produto.categoria}
              </option>
            ))}
          </select>
        </Campo>

        {produtos.isError && (
          <div role="alert" className="rounded-md bg-red-50 px-3 py-2 text-sm text-red-800 dark:bg-red-900/30 dark:text-red-200">
            <p>Não foi possível carregar os produtos. {mensagemDeErro(produtos.error)}</p>
            <button
              type="button"
              onClick={() => void produtos.refetch()}
              className="mt-1 font-medium underline"
            >
              Tentar novamente
            </button>
          </div>
        )}
        {semProdutos && (
          <p role="status" className="rounded-md bg-amber-50 px-3 py-2 text-sm text-amber-800 dark:bg-amber-900/30 dark:text-amber-200">
            Não há produtos ATIVOS. Cadastre um produto (Swagger ou Postman) antes de cadastrar portadores.
          </p>
        )}
        {produtos.isSuccess && produtos.data.totalElementos > catalogo.length && (
          <p role="status" className="text-sm text-slate-600 dark:text-slate-300">
            Mostrando {catalogo.length} de {produtos.data.totalElementos} produtos ativos.
          </p>
        )}

        {cadastro.isError && !temErroDeCampo && (
          <div role="alert" className="rounded-md bg-red-50 px-3 py-2 text-sm text-red-800 dark:bg-red-900/30 dark:text-red-200">
            <p>{mensagemDeErro(cadastro.error)}</p>
            {suporte && <p className="mt-1 text-xs opacity-80">Código de suporte: {suporte}</p>}
          </div>
        )}

        <div className="flex justify-end">
          <button
            type="submit"
            disabled={cadastro.isPending || !produtos.isSuccess || semProdutos}
            className={classeBotaoPrimario}
          >
            {cadastro.isPending ? 'Cadastrando…' : 'Cadastrar'}
          </button>
        </div>
      </form>
    </section>
  )
}

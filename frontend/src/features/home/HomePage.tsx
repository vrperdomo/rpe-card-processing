import { zodResolver } from '@hookform/resolvers/zod'
import { useForm } from 'react-hook-form'
import { Link, useNavigate } from 'react-router-dom'
import { z } from 'zod'
import Campo, { propsAcessiveis } from '../../components/Campo'
import { classeBotaoPrimario, classeCampo, classeCartao } from '../../components/estilos'

const consultaSchema = z.object({
  id: z
    .string()
    .trim()
    .min(1, 'Informe o identificador')
    .refine((valor) => z.guid().safeParse(valor).success, 'Identificador inválido'),
})

type Consulta = z.infer<typeof consultaSchema>

export default function HomePage() {
  const navigate = useNavigate()
  const {
    register,
    handleSubmit,
    formState: { errors },
  } = useForm<Consulta>({ resolver: zodResolver(consultaSchema), defaultValues: { id: '' } })

  return (
    <section>
      <h1 className="text-2xl font-semibold">Portadores</h1>
      <p className="mt-1 text-slate-600 dark:text-slate-300">
        Cadastre um portador e acompanhe a emissão do cartão.
      </p>

      <div className="mt-6 grid gap-4 sm:grid-cols-2">
        <div className={classeCartao}>
          <h2 className="font-semibold">Novo portador</h2>
          <p className="mt-1 text-sm text-slate-600 dark:text-slate-300">
            Cadastra o portador e dispara a emissão do cartão.
          </p>
          <Link to="/portadores/novo" className={`mt-4 ${classeBotaoPrimario}`}>
            Cadastrar portador
          </Link>
        </div>

        <form
          noValidate
          className={classeCartao}
          onSubmit={(e) =>
            void handleSubmit((dados) => navigate(`/portadores/${dados.id.trim()}`))(e)
          }
        >
          <h2 className="font-semibold">Consultar portador</h2>
          <div className="mt-3">
            <Campo id="consulta-id" rotulo="Identificador do portador" erro={errors.id?.message}>
              <input
                type="text"
                autoComplete="off"
                spellCheck={false}
                className={classeCampo}
                {...propsAcessiveis('consulta-id', errors.id?.message)}
                {...register('id')}
              />
            </Campo>
          </div>
          <button type="submit" className={`mt-4 ${classeBotaoPrimario}`}>
            Consultar
          </button>
        </form>
      </div>
    </section>
  )
}

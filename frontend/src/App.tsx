// Casca inicial (#54). As telas reais entram nas issues seguintes: login e rotas protegidas
// (#55) e cadastro de portador + acompanhamento da emissão (#56).
export default function App() {
  return (
    <div className="min-h-screen bg-slate-50 text-slate-900 dark:bg-slate-900 dark:text-slate-100">
      <header className="border-b border-slate-200 bg-white px-6 py-4 dark:border-slate-700 dark:bg-slate-800">
        <h1 className="text-lg font-semibold">RPE Card Processing</h1>
      </header>
      <main className="mx-auto max-w-3xl px-6 py-10">
        <p className="text-slate-600 dark:text-slate-300">
          Cadastro de portadores e acompanhamento da emissão de cartões.
        </p>
      </main>
    </div>
  )
}

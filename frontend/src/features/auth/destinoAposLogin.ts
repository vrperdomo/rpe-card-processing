// Para onde ir depois do login: a rota privada que o usuário tentou abrir (guardada pelo
// RequireAuth no estado de navegação) ou a home. O valor é tratado como não confiável: só caminhos
// internos são aceitos, para o login não virar um redirecionamento aberto ("//site-malicioso").
export function destinoAposLogin(estadoDaNavegacao: unknown): string {
  const origem = (estadoDaNavegacao as { from?: unknown } | null)?.from
  const interno =
    typeof origem === 'string' &&
    origem.startsWith('/') &&
    !origem.startsWith('//') &&
    !origem.startsWith('/\\') &&
    !origem.startsWith('/login')
  return interno ? origem : '/'
}

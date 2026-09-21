// Validação de CPF só para dar retorno imediato ao usuário: quem decide é o backend (Cpf.of), que
// aplica as mesmas regras. Nunca logar nem persistir o valor digitado (CLAUDE.md, regra 4).

export function somenteDigitos(valor: string): string {
  return valor.replace(/\D/g, '')
}

export function cpfValido(valor: string): boolean {
  const digitos = somenteDigitos(valor)
  if (digitos.length !== 11 || /^(\d)\1{10}$/.test(digitos)) return false
  const d10 = digitoVerificador(digitos.slice(0, 9), 10)
  const d11 = digitoVerificador(digitos.slice(0, 9) + d10, 11)
  return digitos.endsWith(`${d10}${d11}`)
}

function digitoVerificador(base: string, pesoInicial: number): number {
  const soma = Array.from(base).reduce((acc, d, i) => acc + Number(d) * (pesoInicial - i), 0)
  const resto = (soma * 10) % 11
  return resto === 10 ? 0 : resto
}

// Máscara enquanto digita: 12345678901 -> 123.456.789-01. Ignora o que passar de 11 dígitos.
export function formatarCpf(valor: string): string {
  const d = somenteDigitos(valor).slice(0, 11)
  const partes = [d.slice(0, 3), d.slice(3, 6), d.slice(6, 9)].filter(Boolean)
  const base = partes.join('.')
  return d.length > 9 ? `${base}-${d.slice(9)}` : base
}

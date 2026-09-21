// Datas de calendário (yyyy-mm-dd) tratadas como texto, sem passar por Date/fuso: um Date em UTC
// deslocaria o dia de nascimento para o dia anterior em fusos a oeste (ex.: Brasil).

const FORMATO_ISO = /^(\d{4})-(\d{2})-(\d{2})$/

export interface DataCivil {
  ano: number
  mes: number
  dia: number
}

export function lerDataIso(valor: string): DataCivil | null {
  const partes = FORMATO_ISO.exec(valor)
  if (!partes) return null
  const data = { ano: Number(partes[1]), mes: Number(partes[2]), dia: Number(partes[3]) }
  // O construtor de Date "rola" datas impossíveis (31/02 vira março): ida e volta detecta isso.
  const conferida = new Date(Date.UTC(data.ano, data.mes - 1, data.dia))
  const existe =
    conferida.getUTCFullYear() === data.ano &&
    conferida.getUTCMonth() === data.mes - 1 &&
    conferida.getUTCDate() === data.dia
  return existe ? data : null
}

export function hojeCivil(agora: Date = new Date()): DataCivil {
  return { ano: agora.getFullYear(), mes: agora.getMonth() + 1, dia: agora.getDate() }
}

function comparar(a: DataCivil, b: DataCivil): number {
  return a.ano - b.ano || a.mes - b.mes || a.dia - b.dia
}

// Estritamente anterior a hoje (o backend usa @Past: a data de hoje já é rejeitada).
export function estaNoPassado(data: DataCivil, hoje: DataCivil): boolean {
  return comparar(data, hoje) < 0
}

export function idadeEmAnos(nascimento: DataCivil, hoje: DataCivil): number {
  const fezAniversario = hoje.mes > nascimento.mes || (hoje.mes === nascimento.mes && hoje.dia >= nascimento.dia)
  return hoje.ano - nascimento.ano - (fezAniversario ? 0 : 1)
}

// 1990-01-31 -> 31/01/1990
export function formatarDataIso(valor: string): string {
  const data = lerDataIso(valor)
  if (!data) return valor
  const dois = (n: number) => String(n).padStart(2, '0')
  return `${dois(data.dia)}/${dois(data.mes)}/${data.ano}`
}

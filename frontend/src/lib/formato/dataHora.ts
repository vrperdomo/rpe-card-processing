// Instante (ISO-8601 com fuso, ex.: 2026-09-21T15:00:00Z) mostrado no fuso do navegador. Diferente
// de lib/validators/data.ts, que trata datas de calendário como texto: aqui há hora, e converter
// para o fuso de quem olha é o comportamento desejado.
export function formatarInstante(instante: string, timeZone?: string): string {
  const data = new Date(instante)
  // Valor fora do contrato: melhor mostrar o texto recebido do que "Invalid Date".
  if (Number.isNaN(data.getTime())) return instante
  return new Intl.DateTimeFormat('pt-BR', {
    dateStyle: 'short',
    timeStyle: 'short',
    ...(timeZone ? { timeZone } : {}),
  }).format(data)
}

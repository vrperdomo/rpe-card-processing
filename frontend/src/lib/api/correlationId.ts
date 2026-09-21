// crypto.randomUUID() só existe em contexto seguro (HTTPS ou localhost). Quem abre a demo por
// http://<IP-da-máquina>:3000 cairia num TypeError antes de qualquer requisição; getRandomValues
// funciona em qualquer contexto, então serve de reserva para gerar um UUID v4.
export function novoCorrelationId(): string {
  if (typeof crypto.randomUUID === 'function') return crypto.randomUUID()

  const bytes = crypto.getRandomValues(new Uint8Array(16))
  bytes.set([((bytes[6] ?? 0) & 0x0f) | 0x40], 6) // versão 4
  bytes.set([((bytes[8] ?? 0) & 0x3f) | 0x80], 8) // variante RFC 4122
  const hex = Array.from(bytes, (byte) => byte.toString(16).padStart(2, '0')).join('')
  return [hex.slice(0, 8), hex.slice(8, 12), hex.slice(12, 16), hex.slice(16, 20), hex.slice(20)].join('-')
}

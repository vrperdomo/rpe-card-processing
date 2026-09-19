# ADR-003 — Estratégia de cache do Produto no Cartão Service

## Contexto

O Cartão Service precisa consultar o Produto Service antes de emitir ou exibir um cartão (para
validar existência/status e obter dados como `bin`, `nome`, `categoria`). Essa consulta acontece
com alta frequência e repetição (o mesmo produto é referenciado por muitos cartões), então uma
chamada HTTP síncrona a cada acesso seria cara e criaria acoplamento de disponibilidade: se o
Produto Service cair, o Cartão não poderia mais responder nada sobre produtos já conhecidos.

O enunciado do desafio destaca "cache Redis eficiente" como critério de avaliação, especificando
explicitamente que não basta um `@Cacheable` simples: é necessário TTL configurável, serialização
JSON explícita, cache negativo (para não martelar o Produto com IDs inexistentes) e tolerância a
falha do Redis.

## Decisão

Implementar **cache-aside manual** (não declarativo) como um adapter dedicado
(`adapters/out/cache/ProdutoCacheAsideClient`), decorando o `ProdutoClient` (a mesma porta usada
pelo client HTTP resiliente) segundo o padrão *Decorator*:

- **Chave:** `produto:v1:{produtoId}` — o prefixo de versão (`v1`) permite trocar o formato
  serializado no futuro sem colidir com entradas antigas.
- **TTL positivo:** 10 minutos (configurável via `rpe.cartao.produto-cache.ttl`). É a rede de
  segurança contra inconsistência quando o evento `ProdutoAtualizado` (evicção ativa, issue #27)
  se perde.
- **Cache negativo:** produto não encontrado (404) também é cacheado, com TTL curto de 60s
  (`rpe.cartao.produto-cache.ttl-negativo`), evitando repetir chamadas ao Produto para IDs
  inválidos ou inexistentes.
- **Distinção "nunca cacheado" x "cacheado como não encontrado":** feita via um wrapper
  serializável (`ProdutoCacheEntry`, com `produto` nulo representando negativo) — a ausência da
  chave no Redis é o único sinal de "nunca consultado", nunca um valor mágico.
- **Falha do Redis nunca derruba o fluxo:** toda leitura/escrita é envolta em
  `try/catch (DataAccessException)`, loga em WARN e segue direto para a chamada real ao Produto
  (ou simplesmente não cacheia o resultado). Redis nunca é ponto único de falha.
- **Erros de indisponibilidade do Produto (`DependenciaIndisponivelException`) nunca são
  cacheados** — cachear isso como "não encontrado" criaria um falso negativo perigoso (rejeitaria
  emissão de cartão por engano enquanto o Produto está apenas fora do ar temporariamente).
- **Serialização:** `GenericJackson2JsonRedisSerializer` para o valor e `StringRedisSerializer`
  para a chave — nunca serialização Java nativa (`JdkSerializationRedisSerializer`), que não é
  legível fora da JVM e quebra em qualquer mudança de classpath.

## Alternativas consideradas

1. **`@Cacheable`/`CacheManager` (Spring Cache abstraction) com `RedisCacheManager`.**
   Rejeitada: o Spring Cache abstraction não suporta nativamente TTLs diferentes por chamada (só
   por "nome de cache" configurado centralmente), o que impediria ter TTL positivo de 10 min e
   negativo de 60s na mesma chave sem duas caches distintas e lógica condicional externa — na
   prática, reimplementaria cache-aside manual por cima de uma abstração que finge ser
   declarativa. O próprio enunciado do desafio pede explicitamente para não parar em
   `@Cacheable`.
2. **Cache negativo com um valor sentinela mágico (ex.: string vazia) em vez de wrapper com campo
   nulo.** Rejeitada: um sentinela é frágil (colide se o formato do DTO mudar) e menos explícito
   que um wrapper tipado.
3. **Não cachear negativo.** Rejeitada: o PRD é explícito sobre evitar martelar o Produto com IDs
   inválidos repetidos (ex.: ataque ou bug no cliente gerando o mesmo ID inexistente muitas
   vezes).

## Consequências

- Positivo: consulta a produtos já vistos passa a ser majoritariamente local (Redis), reduzindo
  latência e carga no Produto Service; comprovado em teste (`ProdutoCacheAsideClientTest`) que a
  segunda chamada não invoca o client HTTP.
- Positivo: Produto Service fora do ar não impede consultas a produtos já cacheados — só afeta
  chamadas em cache miss, que aí sim dependem do Circuit Breaker/Retry do client HTTP (ADR
  futura de resiliência, issue #25).
- Custo: uma janela de inconsistência de até 10 min existe entre o Produto ser atualizado e o
  cache expirar, caso o evento de evicção (issue #27) falhe ao ser processado. Aceitável pelo
  TTL curto e por não ser dado sensível (nome/categoria/status, não PAN/CPF).
- Custo: mais código manual do que um `@Cacheable`, mas ganha em controle explícito de TTL,
  serialização e tolerância a falha — trade-off aceito conscientemente (decisão acima).

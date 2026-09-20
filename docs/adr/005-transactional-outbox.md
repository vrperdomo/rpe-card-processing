# ADR-005 — Transactional Outbox no Portador Service

## Contexto

O cadastro de portador (`POST /api/v1/portadores`) precisa disparar a emissão assíncrona do
cartão via SQS (`CartaoEmissaoSolicitada`, fila `cartao-emissao-queue`, PRD 8.3). O enunciado do
desafio destaca explicitamente o comportamento com **SQS fora do ar** como critério de avaliação
(CLAUDE.md seção 1).

O problema clássico: se o cadastro gravar o portador no Postgres e **depois** chamar
`sqsTemplate.send(...)` diretamente, uma falha do SQS entre os dois passos perde o evento
silenciosamente — o portador existe, mas o cartão nunca é emitido, sem qualquer sinal de erro
para o cliente ou para o time de operação.

## Decisão

**Transactional Outbox** no Portador Service (decisão 19.1/PR-06 do PRD): o cadastro do portador
e o registro do evento de emissão são gravados **na mesma transação** do banco. A publicação
efetiva no SQS é responsabilidade de um processo separado (Outbox Relay, issue #34) que lê a
tabela depois — este ADR cobre apenas a escrita atômica; a leitura/publicação é escopo da próxima
issue.

- **Tabela `outbox_event`** (`id, aggregate_type, aggregate_id, event_type, payload JSONB, status,
  tentativas, proxima_tentativa_em, ultimo_erro, criado_em, publicado_em`), conforme o modelo de
  dados do PRD (seção 7).
- **`CadastrarPortadorUseCase.executar` é `@Transactional`**: chama
  `portadorRepositorio.salvar(...)` e, na sequência, `outboxRepositorio.registrar(...)` — ambos na
  mesma transação Spring. Se qualquer um dos dois falhar, a transação inteira é revertida; **nunca
  existe um portador sem o evento correspondente, nem vice-versa**. Comprovado em teste de
  integração real (`CadastrarPortadorUseCaseAtomicidadeIT`): força a escrita do outbox a falhar e
  verifica, numa transação nova, que o portador não foi persistido.
- **Payload = envelope completo do evento** (`eventId, eventType, eventVersion, occurredAt,
  correlationId, data`), serializado como JSON, no mesmo formato que a issue #34 vai publicar como
  corpo da mensagem SQS — o Relay não precisa reconstruir nada, só ler e enviar.
- **`correlationId` capturado do MDC** no controller (`CorrelationIdFilter`, já usado para
  `ProblemDetailFactory`) e embutido no payload, já que a tabela não tem coluna própria para ele —
  necessário para propagar o rastreamento de ponta a ponta (cadastro → outbox → SQS → consumo no
  Cartão) mesmo com a publicação acontecendo em uma requisição HTTP totalmente diferente.
- **`status` sempre nasce `PENDENTE`, `tentativas = 0`**; as demais colunas de controle de retry
  (`proxima_tentativa_em`, `ultimo_erro`, `publicado_em`) ficam `NULL` até o Relay (#34) atuar.
- **`nomeImpresso`** (campo do evento, não existe no cadastro) é derivado do nome do portador:
  maiúsculo, truncado em 26 caracteres — mesmo limite que o `Cartao.emitir` do Cartão Service já
  valida. Decisão pragmática registrada aqui por não haver requisito explícito no PRD sobre a
  formatação; o Cartão Service continua validando de forma independente (defesa em profundidade).

## Alternativas consideradas

1. **Publicar direto no SQS dentro da transação (`sqsTemplate.send` no meio do `@Transactional`).**
   Rejeitada: I/O de rede dentro de uma transação de banco é uma prática ruim (prende conexão,
   sensível a timeout) e, mais grave, não resolve o problema original — se o SQS falhar, ainda
   assim ou perde o evento (publica fora da transação) ou trava o cadastro inteiro por causa de
   uma dependência externa instável, justamente o cenário que o desafio pede para tolerar.
2. **`@TransactionalEventListener(AFTER_COMMIT)` publicando direto no SQS**, como o Produto Service
   faz para `ProdutoAtualizado` (ADR-003, decisão 19.3). Rejeitada aqui: essa abordagem *não*
   garante entrega — se o SQS estiver fora do ar exatamente no momento do commit, o evento se
   perde, sem outbox para recuperar depois. Aceitável no Produto porque lá o TTL do cache é a rede
   de segurança (perda tolerável); no Portador, a emissão do cartão é o fluxo principal do
   desafio, então a perda não é aceitável — dai a diferença de tratamento entre os dois serviços,
   deliberada e documentada.
3. **Outbox sem coluna `payload` estruturada (guardar só os IDs, montar a mensagem no Relay).**
   Rejeitada: obrigaria o Relay a reconsultar o portador (e possivelmente o produto) no momento da
   publicação, que pode já ter mudado de estado — o payload gravado no momento do cadastro é um
   retrato imutável do evento como ele deveria ter sido publicado originalmente.

## Consequências

- Positivo: o cadastro **nunca** falha por causa do SQS — só depende do Postgres, que já é a
  dependência crítica do próprio cadastro.
- Positivo: a garantia "nunca existe portador sem evento" é testada de ponta a ponta com uma falha
  real injetada, não apenas inferida da anotação `@Transactional`.
- Custo: mensagem publicada em produção não é instantânea — depende da frequência do Relay
  (issue #34). Aceitável e explícito: o PRD já modela a emissão como assíncrona
  (`emissão PENDENTE` na resposta 201).
- Custo: introduz uma tabela e uma etapa extra de leitura/republicação (Relay) em vez de um
  simples `send()`. Trade-off aceito conscientemente — é exatamente o padrão que o desafio pede
  para demonstrar tolerância ao SQS fora do ar.

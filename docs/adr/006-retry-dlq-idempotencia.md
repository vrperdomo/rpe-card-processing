# ADR-006 — Retry, DLQ e idempotência na emissão assíncrona de cartão

## Contexto

O consumo de `CartaoEmissaoSolicitada` (`cartao-emissao-queue`, issue #37) precisa de três garantias
que o desafio pede explicitamente (CLAUDE.md seção 1: "Retry + DLQ" como critério de avaliação):
não perder mensagem, não processar a mesma mensagem duas vezes, e se comportar de forma previsível
quando o Produto Service está fora do ar — sem inventar dado nem travar a fila indefinidamente.

Este ADR documenta as três decisões juntas porque são interdependentes: a estratégia de retry
determina quando uma mensagem é reentregue, o que por sua vez exige idempotência para tolerar
reentregas sem duplicar efeito.

## Decisão

**1. Idempotência em duas camadas independentes** (`EmitirCartaoUseCase`):
- Tabela `mensagem_processada` (`event_id UUID PRIMARY KEY`), verificada e gravada na **mesma
  transação** do cartão — reentrega do mesmo `eventId` vira no-op.
- Constraint `UNIQUE(portador_id, produto_id)` como rede de segurança contra corrida (duas
  entregas concorrentes da mesma mensagem, ou dois eventos distintos para o mesmo par): a violação
  é capturada (`DataIntegrityViolationException`) e tratada como sucesso idempotente, não como erro.

**2. Classificação de erro (CLAUDE.md seção 6.4)**, no `CartaoEmissaoSolicitadaListener`:
- **Transitório** (ex.: `DependenciaIndisponivelException` do circuit breaker do Produto): exceção
  propaga sem tratamento → `AcknowledgementMode.ON_SUCCESS` não confirma → SQS reentrega
  nativamente até `maxReceiveCount` (3) → DLQ automática via redrive policy. Nenhum envio manual.
- **Definitivo** (produto inexistente/`CANCELADO`, payload ilegível, `eventVersion` incompatível):
  envio explícito para a DLQ com `erro-motivo`, método retorna normalmente (ack) — sem redelivery.

**3. Backoff em duas camadas, ambas já implementadas — decisão explícita de não adicionar uma
terceira** (avaliado e confirmado com o Victor em 20/09/2026, rodada de backlog pós-`v1.0.0`):
- **Dentro de uma entrega:** `ProdutoResilienteGateway` já tem Resilience4j `Retry` (3 tentativas,
  200ms inicial, multiplicador ×2, jitter 0.5) + `CircuitBreaker` + `TimeLimiter` na chamada HTTP ao
  Produto — absorve indisponibilidades curtas (blips) sem nunca chegar a precisar de uma nova
  entrega da mensagem.
- **Entre entregas:** o próprio SQS reentrega com o `visibility timeout` configurado na fila
  (fixo, não exponencial) até `maxReceiveCount`, então move para a DLQ.
- **O que foi avaliado e descartado:** backoff *exponencial* **entre** entregas SQS (ex.: 1ª
  reentrega em 5s, 2ª em 20s, 3ª em 60s) não é algo que a anotação `@Retry` do Resilience4j resolve
  — `@Retry` é síncrono, dentro de uma única chamada/thread; não tem como "pausar" uma redelivery
  assíncrona do SQS, que acontece em outro momento, possivelmente em outra instância do consumer.
  A única forma real de implementar isso seria o listener ler o atributo `ApproximateReceiveCount`
  da mensagem e chamar `changeMessageVisibility` manualmente a cada tentativa falha, ajustando o
  timeout da próxima entrega. Avaliado como complexidade real sem ganho claro aqui: já há duas
  camadas de resiliência (retry+CB+timeout na chamada HTTP, que cobre a maioria dos casos reais de
  indisponibilidade curta; redelivery nativa + DLQ para o resto), e o volume de tentativas é baixo
  (3) — o ganho de um backoff exponencial *entre* entregas seria marginal no contexto deste desafio.

**4. Métricas:** `cartao.emitidos` (sucesso) e `cartao.dlq.enviados` (qualquer envio à DLQ,
transitório-esgotado ou definitivo) — expostas via Actuator/Prometheus.

## Alternativas consideradas

1. **Backoff exponencial entre entregas via `changeMessageVisibility` manual.** Avaliada em
   detalhe (seção Decisão acima) — não é redundante tecnicamente com o `@Retry` HTTP existente
   (resolve um problema diferente: espaçamento entre tentativas de entrega, não entre tentativas de
   chamada HTTP), mas o custo de implementação (rastrear `ApproximateReceiveCount`, chamar a API do
   SQS explicitamente, testar contra LocalStack) não se justificou frente ao ganho, dado que as duas
   camadas de resiliência já existentes cobrem o cenário realista de "Produto fora por alguns
   segundos a poucos minutos" que o desafio pede para tolerar.
2. **Idempotência só via constraint única, sem tabela `mensagem_processada`.** Rejeitada: a
   constraint única cobre "mesmo par portador+produto", mas não cobre reentrega do **mesmo**
   `eventId` para um par que falhou por outro motivo transitório entre tentativas — a tabela cobre
   o caso que a constraint não cobre, e vice-versa (defesa em profundidade).
3. **DLQ manual para todo tipo de erro (nunca deixar o SQS reentregar nativamente).** Rejeitada:
   perderia o benefício do backoff nativo do SQS para erros genuinamente transitórios, e obrigaria
   reimplementar a lógica de `maxReceiveCount` na aplicação, que o SQS já resolve de graça via
   redrive policy.

## Consequências

- Positivo: nenhuma mensagem é perdida (outbox no Portador, ADR-005) nem processada duas vezes com
  efeito duplicado, comprovado em teste de integração real (`CartaoEmissaoSolicitadaListenerIT`) —
  incluindo um teste que aguarda uma janela de 3–8s sem envio manual à DLQ para erro transitório.
- Positivo: a decisão de não implementar backoff exponencial entre entregas está registrada com o
  raciocínio completo — não é uma lacuna esquecida, é uma escolha avaliada e revisitável se o volume
  de retries em produção real um dia justificar o custo.
- Custo: o `visibility timeout` fixo da fila (não exponencial) pode gerar reentregas um pouco mais
  próximas umas das outras do que um backoff exponencial faria, num cenário de indisponibilidade
  longa do Produto — mitigado pelo `CircuitBreaker` abrir depois de poucas falhas (`failure-rate-
  threshold: 50%` numa janela de 10 chamadas) e passar a falhar rápido, sem nem tentar a chamada
  HTTP, reduzindo a pressão mesmo sem backoff entre entregas.

## Atualização de 21/09/2026 — estado de falha da emissão (#117)

Uma mensagem que terminava na DLQ deixava o portador `PENDENTE` para sempre: o Portador só via
"existe cartão" ou "ainda não existe". A decisão é o **Cartão registrar a falha e expô-la**; o
Portador a consulta no `/completo`, chamada que já faz ao Cartão. Não há evento de volta.

- **O que é registrado.** Tabela `emissao_falha` (migração `V4` do Cartão): uma linha por portador,
  com `produto_id`, `motivo` e `criado_por` (mesma regra de posse do cartão, ADR-009). Uma nova
  falha do mesmo portador substitui a anterior; uma emissão bem-sucedida a **apaga**, então um
  reprocessamento da DLQ que der certo corrige o estado sozinho.
- **Quando registrar.** Falha **definitiva** (produto inexistente/`CANCELADO`, schema incompatível):
  na hora, antes de enviar à DLQ. Falha **transitória**: só na **última entrega**, que o listener
  reconhece pelo cabeçalho `ApproximateReceiveCount` (`Sqs_Msa_ApproximateReceiveCount`, entregue
  por padrão pelo Spring Cloud AWS) `>= maxReceiveCount`; ele registra e relança, e o SQS move a
  mensagem para a DLQ na entrega seguinte. Payload ilegível **não** é registrado: sem `portadorId`
  não há a quem atribuir a falha (segue só na DLQ).
- **`maxReceiveCount` é configuração duplicada.** `rpe.cartao.mensageria.cartao-emissao-max-receive-count`
  (env `CARTAO_EMISSAO_MAX_RECEIVE_COUNT`, padrão 3) precisa coincidir com o `RedrivePolicy` da fila
  (`infra/localstack/init-queues.sh`). Se for **maior** que o real, a falha transitória não é
  registrada e o portador segue `PENDENTE` (degrada seguro). Se for **menor** e uma nova entrega
  der certo, a emissão apaga a falha registrada cedo demais. Ler o valor da fila em runtime
  (`GetQueueAttributes`) eliminaria a duplicação, mas custaria uma dependência de permissão e de
  disponibilidade do SQS na inicialização; não se justificou.
- **Registrar não pode quebrar o fluxo.** É um efeito colateral informativo: se o banco falhar ao
  registrar, o erro é logado com stack e o fluxo original segue (DLQ, ou relançar para o SQS
  reentregar). O registro roda em transação própria; quando o listener o chama, a transação da
  emissão já foi revertida.
- **Motivo seguro.** Texto para o cliente, sem detalhe interno: para falha definitiva vem da
  `RegraNegocioException` (ex.: "Produto X inexistente ou não ATIVO"); para transitória é fixo
  ("Tentativas de emissão esgotadas: dependência indisponível"). Truncado em 255 caracteres.
- **API.** `GET /api/v1/emissao-falhas/{portadorId}` no Cartão: 200 com a falha, ou 404 se não há
  falha ou se ela é de outro dono. É de uso interno (o Nginx do frontend não o expõe).
- **Métrica.** `cartao.emissao.falhas` com a tag `tipo` (`definitiva` ou `tentativas_esgotadas`).
- **Limitação assumida.** O estado vive no Cartão, então se o Cartão estiver fora o `/completo`
  segue `DESCONHECIDA` (já era assim). A alternativa descartada foi o Cartão publicar
  `CartaoEmissaoFalhou` de volta ao Portador: mais desacoplada, mas exigiria Outbox no Cartão, fila
  nova e consumer no Portador, um custo desproporcional ao ganho aqui.

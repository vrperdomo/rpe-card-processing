#!/usr/bin/env bash
# Bootstrap das filas SQS usadas pelo RPE Card Processing no LocalStack.
# Executado automaticamente pelo LocalStack (hook "ready.d") a cada subida do container.
#
# produto-eventos-queue: Produto Service publica ProdutoAtualizado (issue #21) apos commit;
# Cartao Service consome para evictar o cache (issue #27). DLQ recebe mensagens que falharem
# repetidamente ou que forem definitivamente invalidas (schema incompativel).
#
# cartao-emissao-queue: Portador Service publica CartaoEmissaoSolicitada via Outbox Relay
# (issue #34, PRD 9.1); Cartao Service consome para emitir o cartao (issue futura, PRD 9.2).
# maxReceiveCount=3 conforme PRD secao 9.2.
set -euo pipefail

criar_fila_com_dlq() {
  local fila="$1"
  local dlq="$2"
  local max_receive_count="$3"

  awslocal sqs create-queue --queue-name "$dlq"
  local dlq_arn
  dlq_arn=$(awslocal sqs get-queue-attributes \
    --queue-url "$(awslocal sqs get-queue-url --queue-name "$dlq" --query QueueUrl --output text)" \
    --attribute-names QueueArn --query "Attributes.QueueArn" --output text)

  awslocal sqs create-queue --queue-name "$fila" \
    --attributes "{\"RedrivePolicy\":\"{\\\"deadLetterTargetArn\\\":\\\"${dlq_arn}\\\",\\\"maxReceiveCount\\\":\\\"${max_receive_count}\\\"}\"}"
}

criar_fila_com_dlq produto-eventos-queue produto-eventos-dlq 5
criar_fila_com_dlq cartao-emissao-queue cartao-emissao-dlq 3

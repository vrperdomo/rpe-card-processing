#!/usr/bin/env bash
# Bootstrap das filas SQS usadas pelo RPE Card Processing no LocalStack.
# Executado automaticamente pelo LocalStack (hook "ready.d") a cada subida do container.
#
# produto-eventos-queue: Produto Service publica ProdutoAtualizado (issue #21) apos commit;
# Cartao Service consome para evictar o cache (issue #27). DLQ recebe mensagens que falharem
# repetidamente ou que forem definitivamente invalidas (schema incompativel).
#
# cartao-emissao-queue (Fase 4) sera adicionada em issue futura, junto com o consumer.
set -euo pipefail

awslocal sqs create-queue --queue-name produto-eventos-dlq

DLQ_ARN=$(awslocal sqs get-queue-attributes \
  --queue-url "$(awslocal sqs get-queue-url --queue-name produto-eventos-dlq --query QueueUrl --output text)" \
  --attribute-names QueueArn --query "Attributes.QueueArn" --output text)

awslocal sqs create-queue --queue-name produto-eventos-queue \
  --attributes "{\"RedrivePolicy\":\"{\\\"deadLetterTargetArn\\\":\\\"${DLQ_ARN}\\\",\\\"maxReceiveCount\\\":\\\"5\\\"}\"}"

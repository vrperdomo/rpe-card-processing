#!/usr/bin/env bash
# Cenário de caos: SQS (LocalStack) fora do ar durante o cadastro de um portador.
# Comprova, contra a stack real via docker compose, o comportamento descrito no README
# (seção "Resiliência > SQS fora do ar"): o cadastro continua respondendo 201, o evento
# fica PENDENTE no outbox, e o Outbox Relay publica assim que o SQS volta — sem perda.
#
# Uso: ./scripts/chaos-sqs-down.sh
# Pré-requisito: `docker compose up -d --build --wait` já rodando.

set -euo pipefail

PORTADOR_URL="${PORTADOR_URL:-http://localhost:8082}"
PRODUTO_URL="${PRODUTO_URL:-http://localhost:8081}"
RELAY_TIMEOUT_S="${RELAY_TIMEOUT_S:-60}"

log() { echo "[chaos-sqs-down] $*"; }
falhar() {
  echo "[chaos-sqs-down] FALHOU: $*" >&2
  docker compose start localstack >/dev/null 2>&1 || true
  exit 1
}

# Gera um CPF com dígitos verificadores válidos (o validador @Cpf do Portador rejeita
# sequências aleatórias sem os dígitos corretos calculados pelo algoritmo módulo 11).
gerar_cpf() {
  local base d1 d2 soma peso i digito base10
  base=$(printf '%09d' "$((RANDOM * RANDOM % 1000000000))")
  soma=0; peso=10
  for ((i = 0; i < 9; i++)); do
    digito=${base:$i:1}
    soma=$((soma + digito * peso))
    peso=$((peso - 1))
  done
  d1=$((11 - soma % 11)); [ "$d1" -ge 10 ] && d1=0
  base10="${base}${d1}"
  soma=0; peso=11
  for ((i = 0; i < 10; i++)); do
    digito=${base10:$i:1}
    soma=$((soma + digito * peso))
    peso=$((peso - 1))
  done
  d2=$((11 - soma % 11)); [ "$d2" -ge 10 ] && d2=0
  echo "${base}${d1}${d2}"
}

for bin in curl jq docker; do
  command -v "$bin" >/dev/null 2>&1 || { echo "Requer '$bin' instalado." >&2; exit 1; }
done

log "1/6 — login"
TOKEN=$(curl -sf -X POST "$PORTADOR_URL/api/v1/auth/login" \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"admin123"}' | jq -r .accessToken)
[ -n "$TOKEN" ] && [ "$TOKEN" != "null" ] || falhar "login não retornou accessToken"

log "2/6 — cadastrando produto de teste"
NOME_PRODUTO="Chaos SQS $(date +%s)"
PRODUTO_ID=$(curl -sf -X POST "$PRODUTO_URL/api/v1/produtos" \
  -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" \
  -d "{\"nome\":\"$NOME_PRODUTO\",\"descricao\":\"Produto para teste de caos\",\"categoria\":\"GOLD\",\"bin\":\"453201\"}" \
  | jq -r .id)
[ -n "$PRODUTO_ID" ] && [ "$PRODUTO_ID" != "null" ] || falhar "criação de produto não retornou id"

log "3/6 — derrubando LocalStack (SQS indisponível)"
docker compose stop localstack >/dev/null

CPF_ALEATORIO=$(gerar_cpf)
log "4/6 — cadastrando portador com SQS fora (deve responder 201 mesmo assim)"
HTTP_STATUS=$(curl -s -o /tmp/chaos-sqs-portador.json -w '%{http_code}' -X POST "$PORTADOR_URL/api/v1/portadores" \
  -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" \
  -d "{\"nome\":\"Chaos Teste\",\"cpf\":\"$CPF_ALEATORIO\",\"dataNascimento\":\"1990-01-01\",\"produtoId\":\"$PRODUTO_ID\"}")
[ "$HTTP_STATUS" = "201" ] || falhar "cadastro retornou HTTP $HTTP_STATUS com SQS fora (esperado 201)"
PORTADOR_ID=$(jq -r .id /tmp/chaos-sqs-portador.json)
log "   portador $PORTADOR_ID cadastrado; evento deve estar PENDENTE no outbox"

EMISSAO=$(curl -sf "$PORTADOR_URL/api/v1/portadores/$PORTADOR_ID/completo" -H "Authorization: Bearer $TOKEN" | jq -r .emissao)
[ "$EMISSAO" = "PENDENTE" ] || falhar "emissão esperada PENDENTE com SQS fora, veio '$EMISSAO'"
log "   confirmado: emissao=PENDENTE enquanto o SQS está fora"

log "5/6 — subindo LocalStack de novo"
docker compose start localstack >/dev/null

log "6/6 — aguardando o Outbox Relay publicar (timeout ${RELAY_TIMEOUT_S}s)"
INICIO=$(date +%s)
while true; do
  EMISSAO=$(curl -sf "$PORTADOR_URL/api/v1/portadores/$PORTADOR_ID/completo" -H "Authorization: Bearer $TOKEN" | jq -r .emissao)
  [ "$EMISSAO" = "CONCLUIDA" ] && break
  AGORA=$(date +%s)
  if [ $((AGORA - INICIO)) -ge "$RELAY_TIMEOUT_S" ]; then
    falhar "emissão não chegou a CONCLUIDA em ${RELAY_TIMEOUT_S}s após o SQS voltar (ficou em '$EMISSAO')"
  fi
  sleep 2
done

log "OK — evento sobreviveu à queda do SQS e foi publicado assim que o Relay conseguiu (nenhum cadastro perdido)."

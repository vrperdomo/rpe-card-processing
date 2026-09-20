#!/usr/bin/env bash
# Cenário de caos: Produto Service fora do ar durante a consulta de um cartão.
# Comprova, contra a stack real via docker compose, o comportamento descrito no README
# (seção "Resiliência > Produto Service fora do ar"): sem entrada em cache e com o
# circuito aberto, GET /api/v1/cartoes/{id} nunca inventa dado do produto — responde
# 503 + Retry-After. Assim que o Produto volta, a mesma consulta volta a responder 200.
#
# Uso: ./scripts/chaos-produto-down.sh
# Pré-requisito: `docker compose up -d --build --wait` já rodando.

set -euo pipefail

PORTADOR_URL="${PORTADOR_URL:-http://localhost:8082}"
PRODUTO_URL="${PRODUTO_URL:-http://localhost:8081}"
CARTAO_URL="${CARTAO_URL:-http://localhost:8083}"
EMISSAO_TIMEOUT_S="${EMISSAO_TIMEOUT_S:-60}"
RECUPERACAO_TIMEOUT_S="${RECUPERACAO_TIMEOUT_S:-30}"

log() { echo "[chaos-produto-down] $*"; }
falhar() {
  echo "[chaos-produto-down] FALHOU: $*" >&2
  docker compose start produto-service >/dev/null 2>&1 || true
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

log "1/8 — login"
TOKEN=$(curl -sf -X POST "$PORTADOR_URL/api/v1/auth/login" \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"admin123"}' | jq -r .accessToken)
[ -n "$TOKEN" ] && [ "$TOKEN" != "null" ] || falhar "login não retornou accessToken"

log "2/8 — cadastrando produto de teste"
NOME_PRODUTO="Chaos Produto $(date +%s)"
PRODUTO_ID=$(curl -sf -X POST "$PRODUTO_URL/api/v1/produtos" \
  -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" \
  -d "{\"nome\":\"$NOME_PRODUTO\",\"descricao\":\"Produto para teste de caos\",\"categoria\":\"GOLD\",\"bin\":\"453201\"}" \
  | jq -r .id)
[ -n "$PRODUTO_ID" ] && [ "$PRODUTO_ID" != "null" ] || falhar "criação de produto não retornou id"

CPF_ALEATORIO=$(gerar_cpf)
log "3/8 — cadastrando portador e aguardando a emissão do cartão (timeout ${EMISSAO_TIMEOUT_S}s)"
PORTADOR_ID=$(curl -sf -X POST "$PORTADOR_URL/api/v1/portadores" \
  -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" \
  -d "{\"nome\":\"Chaos Teste\",\"cpf\":\"$CPF_ALEATORIO\",\"dataNascimento\":\"1990-01-01\",\"produtoId\":\"$PRODUTO_ID\"}" \
  | jq -r .id)

INICIO=$(date +%s)
CARTAO_ID=""
while true; do
  CARTAO_ID=$(curl -sf "$CARTAO_URL/api/v1/cartoes?portadorId=$PORTADOR_ID" -H "Authorization: Bearer $TOKEN" \
    | jq -r '.conteudo[0].id // empty')
  [ -n "$CARTAO_ID" ] && break
  AGORA=$(date +%s)
  if [ $((AGORA - INICIO)) -ge "$EMISSAO_TIMEOUT_S" ]; then
    falhar "cartão não foi emitido em ${EMISSAO_TIMEOUT_S}s"
  fi
  sleep 2
done
log "   cartão $CARTAO_ID emitido"

log "4/8 — limpando o Redis para garantir cache frio (docker compose exec redis)"
docker compose exec -T redis redis-cli FLUSHALL >/dev/null

log "5/8 — derrubando o Produto Service"
docker compose stop produto-service >/dev/null

log "6/8 — consultando o cartão sem cache e sem Produto (deve responder 503 + Retry-After)"
HEADERS=$(curl -s -D - -o /dev/null "$CARTAO_URL/api/v1/cartoes/$CARTAO_ID" -H "Authorization: Bearer $TOKEN")
HTTP_STATUS=$(echo "$HEADERS" | head -1 | tr -d '\r' | awk '{print $2}')
[ "$HTTP_STATUS" = "503" ] || falhar "esperado 503 com Produto fora e cache frio, veio HTTP $HTTP_STATUS"
echo "$HEADERS" | grep -qi '^retry-after:' || falhar "resposta 503 não trouxe header Retry-After"
log "   confirmado: 503 + Retry-After — nenhum dado de produto foi inventado"

log "7/8 — subindo o Produto Service de novo"
docker compose start produto-service >/dev/null

log "8/8 — aguardando o circuito fechar e a consulta voltar a 200 (timeout ${RECUPERACAO_TIMEOUT_S}s)"
INICIO=$(date +%s)
while true; do
  HTTP_STATUS=$(curl -s -o /dev/null -w '%{http_code}' "$CARTAO_URL/api/v1/cartoes/$CARTAO_ID" -H "Authorization: Bearer $TOKEN")
  [ "$HTTP_STATUS" = "200" ] && break
  AGORA=$(date +%s)
  if [ $((AGORA - INICIO)) -ge "$RECUPERACAO_TIMEOUT_S" ]; then
    falhar "consulta não voltou a 200 em ${RECUPERACAO_TIMEOUT_S}s após o Produto subir (ficou em HTTP $HTTP_STATUS)"
  fi
  sleep 2
done

log "OK — 503+Retry-After enquanto o Produto estava fora, 200 assim que voltou. Nenhum dado inventado."

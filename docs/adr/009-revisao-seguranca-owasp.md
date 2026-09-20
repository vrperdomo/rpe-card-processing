# ADR-009 — Revisão de segurança (OWASP Top 10)

## Contexto

"SOLID/Clean Code" e ausência de vulnerabilidades óbvias estão entre os critérios de avaliação do
desafio (CLAUDE.md seção 1), e o README já trazia um checklist de segurança (segredos via env, PAN
cifrado/mascarado, CPF mascarado, CVV nunca persistido, BCrypt, JWT, endpoints públicos, sem
concatenação SQL). Esse checklist lista **features implementadas**, mas nunca foi confrontado
sistematicamente contra uma taxonomia de risco — corre o risco de dar uma falsa sensação de
completude só por listar o que já existe, sem nomear o que **não** existe.

Esta revisão substitui a lista solta por um walkthrough real das 10 categorias do OWASP Top 10
(2021) contra o código dos 3 serviços, com evidência concreta (arquivo/linha) por categoria — e,
onde há lacuna real, ela é nomeada como lacuna, não escondida atrás de itens que já estão prontos.

## Decisão

**Veredito por categoria, com evidência:**

| # | Categoria | Veredito | Evidência |
|---|---|---|---|
| A01 | Broken Access Control | ⚠️ Lacuna | `SecurityConfig` dos 3 serviços só verifica `anyRequest().authenticated()` — nenhum `@PreAuthorize`/`hasRole` em todo o código. Qualquer JWT válido lê/altera qualquer `portadorId`/`cartaoId`, não só os próprios (sem verificação de posse de recurso). |
| A02 | Cryptographic Failures | ✅ Mitigado | `PanCriptografoAesGcm` usa AES-256-GCM autenticado (`Encryptors.stronger`), IV aleatório por chamada, chave via PBKDF2 (senha+salt em env). Hash SHA-256+pepper separado para unicidade sem decifrar. `.env.example` marca os defaults como `dev-only-insecure`. Sem TLS (loopback local via Compose — fora do escopo do desafio). |
| A03 | Injection | ✅ Mitigado | Único `@Query nativeQuery=true` do projeto (`OutboxEventJpaRepository`, `SELECT ... FOR UPDATE SKIP LOCKED`) é 100% parametrizado. Resto é Spring Data derivado. Nenhum `createNativeQuery`/`createQuery` dinâmico. |
| A04 | Insecure Design | ⚠️ Parcial | Pontos fortes: idempotência real (`mensagem_processada` + UNIQUE), Transactional Outbox, Bean Validation em todo DTO de entrada. Lacuna: nenhum rate limiting em lugar nenhum — `/api/v1/auth/login` aceita tentativas ilimitadas. |
| A05 | Security Misconfiguration | ⚠️ Parcial | `management.endpoints.web.exposure.include` restrito a `health,info,metrics,prometheus` nos 3 serviços (sem `/env`, `/beans`). Dockerfile roda `USER rpe` (não-root) + `HEALTHCHECK`. Lacuna: CORS é decisão documentada no CLAUDE.md mas **não implementada em código** — não há `CorsConfiguration`/`@CrossOrigin` em nenhum serviço, porque o frontend (F7) nunca foi construído nesta entrega. |
| A06 | Vulnerable/Outdated Components | ✅ Mitigado (processo) | Trivy filesystem scan (CRITICAL/HIGH) + `dependency-review-action` em todo PR (`security.yml`). Dependabot semanal (maven/docker/github-actions), agora com `target-branch: develop` (ADR de infra desta mesma rodada de backlog). |
| A07 | Identification and Authentication Failures | ⚠️ Parcial | JWT HS256, `iss`/`aud` validados, segredo via env, expiração curta, senha do seed em BCrypt. Lacunas: sem rate limiting/lockout de login (mesma raiz do A04); `AutenticarUseCase`/`JwtAuthEntryPoint` nunca logam uma tentativa de autenticação falha. |
| A08 | Software and Data Integrity Failures | ✅ Mitigado | Actions de terceiros fixadas por SHA com comentário de versão em todos os workflows. Transactional Outbox garante que o evento de emissão nunca é publicado sem o registro correspondente já commitado na mesma transação. |
| A09 | Security Logging and Monitoring Failures | ⚠️ Parcial | Logs estruturados em JSON (ECS), `correlationId`/`eventId` no MDC, métricas customizadas (`cartao.emitidos`, `cartao.dlq.enviados`, `outbox.pendentes`, `outbox.falhas`). Lacuna: nenhum evento de segurança é logado explicitamente — um 401/403 ou uma tentativa de força bruta em `/auth/login` não deixa rastro nos logs da aplicação. |
| A10 | Server-Side Request Forgery | ✅ Mitigado por design | As 3 URLs de cliente HTTP entre serviços são `@ConfigurationProperties` validadas (`@NotBlank`), fixadas via env/Compose — nunca construídas a partir de input de request do cliente. Nenhum endpoint aceita URL/host como parâmetro. |

**Lacunas reais assumidas como backlog consciente (não escondidas, não bloqueiam o release):**

1. **Sem autorização por posse de recurso (A01)** — IDOR potencial: qualquer usuário autenticado
   acessa qualquer `portadorId`/`cartaoId`. Risco prático baixo hoje (desafio local, usuário seed
   único, sem multiusuário real) — seria um problema real em produção com múltiplos usuários.
2. **Sem rate limiting em `/api/v1/auth/login` (A04/A07)** — aceita tentativas ilimitadas. Risco
   prático baixo sem exposição pública real; seria brute-force real em produção.
3. **Sem logging de eventos de autenticação falha (A09)** — `JwtAuthEntryPoint`/
   `JwtAccessDeniedHandler` só respondem 401/403, nunca logam. Um ataque de força bruta não deixa
   rastro nos logs da aplicação hoje.
4. **CORS documentado, não implementado (A05)** — decisão do CLAUDE.md ("CORS restrito à origem do
   frontend") nunca foi codificada porque o frontend (F7) está fora desta entrega. Não é
   vulnerabilidade ativa (sem frontend, sem cliente browser), mas é uma divergência real entre
   documentação e código.

## Alternativas consideradas

1. **Manter só o checklist de features do README.** Rejeitada: lista o que existe, não audita
   contra uma taxonomia de risco — não distingue "implementado e correto" de "nunca precisou ser
   implementado porque não há esse tipo de ataque possível aqui" de "é uma lacuna real".
2. **Pentest formal / ferramenta automatizada (OWASP ZAP) contra a API rodando.** Considerada, mas
   desproporcional ao prazo e ao escopo de um desafio técnico local sem exposição pública real —
   registrada aqui como evolução natural caso o projeto vá a produção de verdade.
3. **Corrigir as 4 lacunas agora em vez de só documentá-las.** Rejeitada por ora: rate limiting,
   autorização por posse de recurso e logging de eventos de segurança são mudanças de escopo maior
   (a primeira pede uma dependência nova tipo Bucket4j; a segunda pede repensar o modelo de
   autorização inteiro) — desproporcionais ao risco real no contexto atual (ambiente local, sem
   exposição pública). Documentar honestamente como backlog é mais correto do que inflar o escopo
   desta entrega ou esconder a lacuna atrás de um checklist incompleto.

## Consequências

- Positivo: qualquer avaliador que leia este ADR sabe exatamente o que foi verificado, como, e o
  que continua em aberto — sem alegações vagas de "seguro" sem evidência.
- Positivo: as 4 lacunas viram itens de backlog nomeados, não surpresas descobertas depois.
- Custo: este documento não substitui um pentest real nem uma análise de ameaças formal (STRIDE
  etc.) — é uma revisão de código dirigida por uma taxonomia conhecida, com o nível de rigor
  proporcional a um desafio técnico, não a um sistema em produção real.

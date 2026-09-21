# CLAUDE.md — RPE Card Processing Platform

> Instruções permanentes para o Claude (Claude Code / Claude.ai) neste repositório.
> Leia este arquivo **inteiro** antes de qualquer tarefa. A especificação funcional completa está em [`PRD.md`](./PRD.md).

---

## 0. Regras invioláveis (leia primeiro)

1. **Idioma:** toda comunicação, perguntas, respostas, comentários de PR, ADRs e documentação em **Português do Brasil**. Identificadores de código (classes, métodos, variáveis) seguem a linguagem de domínio em português (`Portador`, `emitirCartao`) e termos técnicos consagrados em inglês (`Repository`, `Controller`, `Listener`).
2. **Git — o Claude NUNCA executa:** `git commit`, `git push`, `git merge`, `git rebase`, `git tag`, `git reset --hard`, `git push --force`, `gh pr create`, `gh pr merge`, `gh release create`. Todos os commits são feitos pelo **Victor**.
   - Ao terminar uma unidade de trabalho, o Claude **gera a mensagem de commit** (seção 5) e **para**.
   - Comandos de leitura são permitidos: `git status`, `git diff`, `git log`, `git branch`.
3. **Nunca** criar, expor ou commitar segredos (tokens, senhas, chaves JWT, credenciais AWS). Usar `.env` (ignorado) e `.env.example` (com valores fictícios).
4. **Nunca** persistir PAN em claro nem CVV. **Nunca** logar CPF ou PAN completos.
5. **Não sair do escopo da fase atual** (seção 3). Se algo pertence a outra fase, registrar como sugestão de issue.
6. **Nada é "pronto" sem teste.** Toda regra de negócio nova vem acompanhada de teste.
7. **Na dúvida, perguntar.** Ambiguidade de negócio ou decisão arquitetural → fazer pergunta objetiva com opções e recomendação, antes de codificar.
8. **Não alterar** `PRD.md`, ADRs aceitos, workflows de CI ou `docker-compose.yml` sem explicitar a mudança e o motivo.

---

## 1. Contexto do projeto

Desafio técnico **Java Software Senior — RPE (B.U. Processamento)**: 3 microserviços (Produto, Portador, Cartão) com REST, SQS (LocalStack), Redis, PostgreSQL e Docker Compose, mais um frontend React como diferencial.

**Prioridades do avaliador:** SOLID/Clean Code · exceções globais e HTTP semântico · **Retry + DLQ** · **cache Redis eficiente** · Docker em um comando · Testcontainers · README profissional · **comportamento com SQS fora e Produto offline**.

- GitHub: `https://github.com/vrperdomo` · Repositório: `vrperdomo/rpe-card-processing`
- Autor: Victor Rodrigues

---

## 2. Stack e versões

| Item | Escolha |
|---|---|
| Java | 21 |
| Spring Boot | 3.5.x (linha 3.x, confirmar último patch) |
| Mensageria | Spring Cloud AWS 3.x (SQS) + LocalStack |
| Resiliência | Resilience4j (spring-boot3) |
| Persistência | PostgreSQL 16 + Spring Data JPA + Flyway |
| Cache | Redis 7 + Spring Data Redis |
| Segurança | Spring Security + OAuth2 Resource Server (JWT) |
| API Docs | springdoc-openapi 2.x |
| Mapeamento | MapStruct |
| Testes | JUnit 5, AssertJ, Mockito, Testcontainers, WireMock, Awaitility, ArchUnit, JaCoCo |
| Frontend | React 19, Vite, TypeScript strict (**6.0.x**: o `typescript-eslint` só aceita `<6.1.0`), Tailwind, TanStack Query, React Hook Form + Zod, Vitest + Testing Library (Playwright em backlog) |

Antes de adicionar **qualquer** dependência: justificar, verificar compatibilidade com Boot 3.5 e fixar a versão no POM agregador (`dependencyManagement`) ou `package.json` (versão exata, com `package-lock.json` commitado).

Majors que tiram o projeto da linha da stack (Spring Boot 4.x, springdoc 3.x, Java 25...) **não são aceitos**: fechar o PR do Dependabot correspondente (`@dependabot ignore this major version`).

---

## 3. Fases (seguir em ordem)

> ✅ **Estado em 21/09/2026: `v1.0.0` (20/09), `v1.1.0` e `v1.2.0` (21/09) entregues**, dentro do prazo final (21/09 às 13h). A seção 3.1 registra o que entrou em cada versão e o que segue em backlog. O cronograma diário abaixo está **superado** e mantido só como referência histórica do escopo completo do desafio.
> O Claude deve sempre priorizar **entregar funcionando** antes de sofisticar. Itens "Could" nunca bloqueiam release. Se uma tarefa ameaçar o prazo ou uma release, avisar imediatamente e propor corte.

| Fase | Milestone | Foco | Release | Dia (original, superado) |
|---|---|---|---|---|
| F0 | M0 - Fundação | Repo, CI base, Compose, skeletons, devcontainer | v0.1.0 | 17/09 |
| F1 | M1 - Produto Service | CRUD, auditoria, JWT (resource server), evento `ProdutoAtualizado` | v0.2.0 | 17/09 |
| F2 | M2 - Cartão Core | Domínio, client Produto, Redis + evicção por evento, Resilience4j | v0.3.0 | 18/09 |
| F3 | M3 - Portador Service | Emissão de JWT, cadastro (≥ 18 anos), Outbox | v0.4.0 | 18/09 |
| F4 | M4 - Emissão Assíncrona | Consumer SQS, retry, DLQ, idempotência | v0.5.0 | 19/09 |
| F5 | M5 - Agregação & Resiliência | Consulta completa degradável, caos, Postman, E2E | v0.6.0 | 19/09 |
| F6 | M6 - Release 1.0 | Hardening, README, ~~demo gravada~~ (backlog), **release (ENTREGA)** | **v1.0.0** ✅ | 20/09 |
| F7 | M7 - Frontend | React enxuto: login, cadastro de portador, detalhe com polling (#54–#56) ✅; painel de saúde (#57) e Playwright (#58) em backlog | v1.1.0 ✅ | 21/09 |

### 3.1 Replanejamento de escopo (20/09/2026) e estado da entrega (21/09/2026)

**Motivo do replanejamento (20/09, ~04h20):** auditoria do código real (não dos labels do GitHub, que estavam desatualizados) mostrou que o cronograma diário original já não era alcançável — F4 (o consumer SQS que efetivamente emite o cartão) estava em 0%, apesar de domínio/persistência/outbox prontos. O Victor pediu para recalcular o escopo para caber até **21/09 13h**.

**Critério de corte (segue valendo para qualquer nova pressão de prazo):** mantém-se tudo que aparece nas "Prioridades do avaliador" (seção 1) — SOLID, exceções/HTTP semântico, Retry+DLQ, cache Redis, Docker em 1 comando, Testcontainers, README profissional, comportamento com SQS fora/Produto offline. Corta-se automação/processo que não muda o que o avaliador vê rodando a aplicação.

**Resultado:**

| Versão | Data | Conteúdo |
|---|---|---|
| **v1.0.0** | 20/09 | `ProdutoAtualizado` publicado após commit; F4 completo (listener SQS, idempotência, classificação transitório × definitivo, DLQ, métricas); `GET /portadores/{id}/completo` com resposta degradada; README profissional; release manual. |
| **v1.2.0** | 21/09 | Segurança (ADR-009): posse de recurso em Portador e Cartão (#121) e log de 401/403 (#120). Estado de falha da emissão: `FALHOU` com motivo, registrado pelo Cartão e exposto no `/completo` e na UI (#117, ADR-006). |
| **v1.1.0** | 21/09 | Frontend F7 (#54–#56, ADR-008); limite de tentativas no login (ADR-009); e os itens que tinham sido cortados do v1.0.0 e acabaram entrando: ArchUnit, scripts de caos, Postman + Newman, `e2e.yml`, contrato de evento via JSON Schema, logs JSON, CODEOWNERS/templates, ADR-006 e ADR-009. |

**Segue em backlog** (issues abertas; nenhum bloqueia release):
- `.devcontainer` (Codespaces) e demo gravada — o Docker Compose local já cobre a demonstração.
- `docker-publish.yml` (GHCR) e `release.yml` — a release segue manual (README, seção Release).
- Paralelização das chamadas do `/completo` (hoje sequenciais: Portador → Cartão → Produto).
- `GET /portadores` (listagem). O estado de falha da emissão (`FALHOU`, #117) foi entregue no Cartão, no Portador e na UI.
- `/api/v1/admin/dlq` (Could); painel de saúde (#57) e Playwright (#58) no frontend.
- Branch protection em `develop` e `main` (#65).
- Segurança (ADR-009): as lacunas nomeadas foram todas fechadas (log de 401/403 #120 e posse de recurso #121). Sobra só a limitação assumida de um único usuário seed.

**Descartado por decisão (não é backlog):** backoff exponencial **entre** entregas SQS — usa-se o `visibility timeout` nativo + `maxReceiveCount` até a DLQ, mais o retry do cliente HTTP (ADR-006).

### Decisões já tomadas (não reabrir sem motivo forte)
- **JWT:** o Portador emite; Produto e Cartão validam (resource servers). Segredo via env.
- **Cache:** cache-aside no Cartão com TTL de 10 min e cache negativo de 60 s. O evento `ProdutoAtualizado` (after-commit) remove a chave. O TTL é a rede de segurança.
- **Consulta agregada:** resposta degradada (200 + `avisos`); 503 só se o portador não puder ser lido; chamadas **sequenciais** por decisão de 20/09 (seção 3.1); a paralelização está em backlog.
- **Regras:** um cartão por par portador + produto; portador com ≥ 18 anos.
- **Frontend:** token JWT **apenas em memória** (nunca `localStorage`/cookie); o Nginx é a única origem do browser e faz proxy de `/api/v1/*`, então **nenhum serviço tem CORS** (ADR-008).
- **Login:** no máximo 5 tentativas por minuto por IP; depois `429` + `Retry-After` (ADR-009). O IP vem do `X-Forwarded-For`, que o Nginx **sobrescreve** (não acrescenta) para o cliente não forjá-lo.
- **Demo:** custo zero, via Docker Compose local (`docker compose up --build`). GitHub Codespaces (`.devcontainer`), demo gravada e imagens no GHCR seguem em backlog (seção 3.1). Sem hospedagem paga nem cadastro de cartão de crédito.
- **Release:** `release/x.y.z` sai da `develop`, PR para `main` com **merge commit** (nunca squash), CI verde, tag anotada em `main` e back-merge na `develop` (README, seção Release).

**Ao iniciar uma sessão**, o Claude deve:
1. Rodar `git status` e `git branch --show-current`.
2. Confirmar com o Victor **qual issue/fase** está sendo trabalhada.
3. Verificar se a branch segue o padrão (seção 4); se não, sugerir o comando de criação (sem executar checkout destrutivo).

---

## 4. Git Flow

```
main ──●────────────────────●── (tags vX.Y.Z)
        \                  /
develop ─●──●──●──●──●──●─●──
             \   /  \   /
   feature/12-produto-crud  feature/15-cartao-dominio
```

| Branch | Sai de | Volta para | Merge |
|---|---|---|---|
| `feature/<issue>-<slug>` | develop | develop | Squash |
| `bugfix/<issue>-<slug>` | develop | develop | Squash |
| `docs/<issue>-<slug>`, `ci/<issue>-<slug>`, `chore/<issue>-<slug>` | develop | develop | Squash |
| `release/<x.y.z>` | develop | main + develop | Merge commit + tag |
| `hotfix/<x.y.z>` | main | main + develop | Merge commit + tag |

- Slug em minúsculas, hífens, sem acentos: `feature/24-transactional-outbox`.
- Uma branch = uma issue = um PR pequeno (ideal < 400 linhas alteradas).
- O `pr-lint` só aceita branches `feature|bugfix|docs|ci|chore/<issue>-<slug>` e `release|hotfix/<x.y.z>` (Dependabot é isento): **nunca** abrir PR com origem em `develop` ou `main`.
- O Claude **sugere** os comandos; o Victor executa:
  ```bash
  git switch develop && git pull
  git switch -c feature/24-transactional-outbox
  ```

---

## 5. Mensagens de commit (gerar, nunca executar)

**Formato — Conventional Commits:**
```
<tipo>(<escopo>): <descrição imperativa, minúscula, sem ponto, ≤ 72 chars>

<corpo explicando o QUÊ e o PORQUÊ, quebrado em 72 colunas>

Refs: #<issue>
```

- **Tipos:** `feat` `fix` `refactor` `perf` `test` `docs` `build` `ci` `chore` `style` `revert`
- **Escopos:** `produto` `portador` `cartao` `frontend` `infra` `ci` `docs` `deps`
- Mudança incompatível → rodapé `BREAKING CHANGE: ...`
- Preferir **commits atômicos**: se o trabalho tiver partes independentes, propor vários commits com os arquivos de cada um.

**Modelo de entrega ao final de cada tarefa:**

````markdown
### ✅ Pronto para commit

**Branch:** `feature/24-transactional-outbox`

**Arquivos para stage:**
```bash
git add services/portador-service/src/main/java/.../outbox \
        services/portador-service/src/main/resources/db/migration/V3__outbox.sql
```

**Mensagem:**
```bash
git commit -m "feat(portador): adicionar transactional outbox para emissão de cartão" \
  -m "Grava o portador e o evento CartaoEmissaoSolicitada na mesma transação,
garantindo que a indisponibilidade do SQS não gere perda de eventos." \
  -m "Refs: #24"
```

**Validação antes do commit:**
```bash
./mvnw -pl services/portador-service spotless:apply verify
```

**Título sugerido do PR:** `feat(portador): transactional outbox para emissão de cartão`
````

---

## 6. Arquitetura e padrões de código

### 6.1 Hexagonal enxuta (por serviço)
```
br.com.rpe.<servico>
├── domain/              # SEM Spring, SEM JPA. Entidades, VOs, regras, exceções de domínio
├── application/         # Casos de uso (1 por classe) + portas in/out (interfaces)
├── adapters/in/web/     # Controllers, DTOs (records), mappers, ControllerAdvice
├── adapters/in/messaging/
├── adapters/out/persistence/   # Entidades JPA separadas do domínio
├── adapters/out/http/
├── adapters/out/messaging/
├── adapters/out/cache/
└── config/
```
Regras verificadas por **ArchUnit**: `domain` não depende de nada externo; `application` depende só de `domain`; adapters não se chamam entre si.

### 6.2 Regras de código
- **SOLID** e métodos curtos (≤ ~20 linhas); classes com uma responsabilidade.
- **Injeção por construtor**, campos `final`. Proibido `@Autowired` em campo.
- **DTOs como `record`**, validados com Bean Validation (`@Valid`, `@NotBlank`, validador customizado `@Cpf`).
- **Value Objects** para `Cpf`, `Pan`, `Validade` — validação no construtor.
- **Nunca** expor entidade JPA em controller.
- `Optional` só como retorno; nunca `Optional.get()` sem verificação.
- Datas com `Instant`/`OffsetDateTime` (UTC); `Clock` injetado para testabilidade.
- Sem `System.out`, sem `printStackTrace`, sem exceções engolidas.
- Sem lógica em construtores de entidades JPA; `equals/hashCode` por ID de forma segura.
- Paginação obrigatória em listagens (`Pageable`, tamanho máximo limitado).
- `@Transactional` apenas na camada de aplicação, nunca no controller; `readOnly = true` em consultas.
- Evitar N+1: revisar consultas, usar projeções/`@EntityGraph` quando necessário.
- Configurações tipadas com `@ConfigurationProperties` (records) + `@Validated`.
- Formatação: `./mvnw spotless:apply` antes de propor commit.

### 6.3 Exceções e HTTP
- `GlobalExceptionHandler` (`@RestControllerAdvice`) retornando **`ProblemDetail` (RFC 9457)** com `correlationId` e `timestamp`.
- Hierarquia: `DomainException` → `RecursoNaoEncontradoException` (404), `ConflitoException` (409), `RegraNegocioException` (422); `DependenciaIndisponivelException` (503 + `Retry-After`).
- Nunca vazar stack trace ou mensagem interna na resposta.
- Mensagens ao cliente **sempre em português**: as do Bean Validation e as dos erros do Spring MVC ficam no `messages.properties` de cada serviço (sem `messages_en`). Restrição nova do Jakarta Validation sem tradução quebra `MensagensEmPortuguesTest`.
- `POST` de criação → `201` + header `Location`.

### 6.4 Mensageria (SQS)
- **Produtor (Portador):** somente via **Transactional Outbox**. Proibido `sqsTemplate.send()` dentro do caso de uso.
- **Consumidor (Cartão):**
  - `AcknowledgementMode.ON_SUCCESS`.
  - **Idempotência obrigatória** (`mensagem_processada` na mesma transação).
  - Classificar erros: `ErroTransitorio` (lança → redelivery → DLQ via `maxReceiveCount`) × `ErroDefinitivo` (DLQ direto com motivo + ack).
  - Propagar `correlationId` de atributo SQS para o MDC.
- Eventos versionados (`eventVersion`) e validados contra JSON Schema em testes.
- Evento **não** carrega CPF.

### 6.5 Cache (Redis)
- Cache-aside no Cartão, chave `produto:v1:{id}`, TTL configurável; cache negativo para 404 com TTL curto.
- Serialização JSON explícita (`GenericJackson2JsonRedisSerializer` ou serializer tipado) — nunca serialização Java nativa.
- `CacheErrorHandler` tolerante: Redis fora não quebra o fluxo.
- Testes devem **provar** o hit de cache (WireMock verificando número de chamadas).

### 6.6 Resiliência HTTP
- `RestClient` com timeouts de conexão e leitura explícitos.
- Resilience4j: `TimeLimiter` → `Retry` (apenas exceções transitórias, backoff exponencial com jitter) → `CircuitBreaker`.
- Fallback nunca inventa dados: usa cache ou sinaliza indisponibilidade.

### 6.7 Segurança
- JWT com segredo/chave via variável de ambiente; expiração curta; `iss` e `aud` validados.
- Senhas com BCrypt. Usuário seed apenas para ambiente local, documentado.
- Endpoints públicos apenas: `/api/v1/auth/login`, `/actuator/health/**`, `/v3/api-docs/**`, `/swagger-ui/**`.
- CORS restrito à origem do frontend.
- PAN cifrado (AES-GCM, chave via env) + `pan_hash` (SHA-256 com pepper) para unicidade; respostas sempre mascaradas.
- CPF mascarado em logs (`***.456.789-**`) — usar um utilitário central, não máscara ad hoc.
- Validar todo input; nunca concatenar SQL.

### 6.8 Observabilidade
- Filtro de `X-Correlation-Id` (gera se ausente) → MDC → propagado em chamadas HTTP e atributos SQS.
- Logs estruturados (JSON) com `service`, `correlationId`, `eventId`.
- Métricas custom: `outbox.pendentes`, `outbox.falhas`, `cartao.emitidos`, `cartao.dlq.enviados`, `cartao.emissao.falhas`, `produto.cache.hit/miss`.
- Actuator: `health` (liveness/readiness), `info`, `metrics`, `prometheus`.

---

## 7. Testes

| Tipo | Onde | Obrigatório |
|---|---|---|
| Unitário | `domain`, `application` | Sempre |
| `@WebMvcTest` | controllers | Status HTTP, validação, 401/403 |
| `@DataJpaTest` + Testcontainers | repositórios | Migrações e constraints |
| `@SpringBootTest` + Testcontainers | fluxos críticos | Postgres, Redis, LocalStack reais |
| WireMock | chamadas ao Produto | Offline, 404, lentidão |
| Awaitility | assíncrono | Nunca `Thread.sleep` |
| ArchUnit | arquitetura | Um teste por serviço |
| Vitest + Testing Library | frontend | Comportamento por tela, mensagens de erro, contrato da API validado com Zod |

- Padrão **Given / When / Then**, nomes descritivos: `deveEnviarParaDlqQuandoProdutoInexistente()`.
- Classe base de integração com containers reutilizados (`@ServiceConnection` quando aplicável).
- **Cobertura mínima JaCoCo: 80%** em `domain` + `application` (gate no CI).
- Comandos:
  ```bash
  ./mvnw verify                                   # tudo
  ./mvnw -pl services/cartao-service verify       # um serviço
  ./mvnw -pl services/cartao-service test -Dtest=EmissaoCartaoListenerIT
  cd frontend && npm run lint && npm run typecheck && npm test
  ```

---

## 8. Code Review obrigatório (antes de toda mensagem de commit)

O Claude executa um **auto code review** em todo diff (`git diff` / `git diff --staged`) e entrega no formato abaixo. Se houver item 🔴, **não gerar mensagem de commit** até ser corrigido.

### Dimensões verificadas
- **Segurança:** injeção (SQL/JPQL), XSS, CSRF, falhas de autenticação/autorização, segredos no código, desserialização insegura, SSRF, dados sensíveis em logs (CPF/PAN), OWASP Top 10.
- **Performance:** N+1, consultas sem limite, índices ausentes, alocações desnecessárias, vazamento de recursos, pool de conexões, TTL de cache.
- **Correção:** null/vazio/limites, concorrência e condições de corrida (outbox, consumer), idempotência, propagação de erro, transações, fuso horário.
- **Resiliência:** timeouts, retry só em erros transitórios, circuit breaker, DLQ, comportamento com SQS/Redis/Produto fora.
- **Manutenibilidade:** nomes, responsabilidade única, duplicação, fronteiras hexagonais, cobertura de testes, documentação do não óbvio.

### Formato de saída
```markdown
## Code Review: <branch / escopo>

### Resumo
<1–2 frases>

### Problemas críticos
| # | Arquivo | Linha | Problema | Severidade |
|---|---------|-------|----------|------------|

### Sugestões
| # | Arquivo | Linha | Sugestão | Categoria |
|---|---------|-------|----------|-----------|

### Pontos positivos
- ...

### Veredito
Aprovar / Solicitar mudanças / Precisa discussão
```

---

## 9. Definition of Done (por issue)

- [ ] Critérios de aceite da issue atendidos
- [ ] Testes unitários e de integração passando localmente (`./mvnw verify`)
- [ ] Cobertura ≥ 80% nas camadas de negócio
- [ ] `spotless:apply` executado
- [ ] OpenAPI atualizado (anotações/exemplos)
- [ ] Sem segredos, sem dados sensíveis em log
- [ ] Code review (seção 8) sem itens críticos
- [ ] README/ADR atualizados quando houver decisão técnica
- [ ] Postman Collection atualizada quando houver endpoint novo
- [ ] Mensagem de commit e título de PR entregues ao Victor

---

## 10. GitHub Actions — regras para o Claude ao editar workflows

- `permissions:` mínimas por workflow/job (default `contents: read`).
- Actions de terceiros **fixadas por SHA** com comentário da versão (`# v4.2.2`).
- `concurrency: { group: ${{ github.workflow }}-${{ github.ref }}, cancel-in-progress: true }` em PRs.
- `timeout-minutes` em todo job.
- `dorny/paths-filter` para rodar só o serviço alterado; matriz por serviço.
- Cache Maven/npm via `setup-java`/`setup-node`.
- Nunca imprimir segredos; nunca usar `pull_request_target` com checkout de código do PR.
- Validar localmente com `act` (opcional) ou `actionlint` antes de propor.
- Workflows existentes: `ci-backend`, `ci-frontend`, `codeql`, `security`, `e2e`, `pr-lint`. Planejados (backlog): `docker-publish`, `release`.
- `github/codeql-action` (`init`, `analyze`, `upload-sarif`) **sempre na mesma versão/SHA**: o `analyze` aborta se a configuração gravada pelo `init` for de outra versão. O Dependabot as agrupa (`groups.codeql-action`).
- O Dependabot lê o `dependabot.yml` da branch **padrão** (`main`): mudanças nele só passam a valer depois do próximo release.

---

## 11. Docker e ambiente local

```bash
docker compose up -d --build --wait     # sobe tudo (sem .env: o compose tem padrões dev-only; cp .env.example .env só para sobrescrever)
docker compose ps
docker compose logs -f cartao-service
docker compose down -v                  # derruba e limpa volumes
```

| Serviço | URL |
|---|---|
| Frontend | http://localhost:3000 |
| Produto (Swagger) | http://localhost:8081/swagger-ui.html |
| Portador (Swagger) | http://localhost:8082/swagger-ui.html |
| Cartão (Swagger) | http://localhost:8083/swagger-ui.html |
| LocalStack | http://localhost:4566 |
| Postgres | localhost:5432 |
| Redis | localhost:6379 |

Regras dos Dockerfiles: multi-stage, imagem final JRE (Eclipse Temurin 21), **usuário não-root**, `HEALTHCHECK`, flags de memória para container (`-XX:MaxRAMPercentage=75`), sem segredos em `ARG`/`ENV` fixos.

Cenários de caos (Fase 5):
```bash
./scripts/chaos-sqs-down.sh       # para LocalStack, cadastra, sobe, verifica emissão
./scripts/chaos-produto-down.sh   # para Produto, verifica cache/CB/DLQ
```

---

## 12. Documentação

- `README.md`: visão geral, arquitetura (Mermaid), setup em 1 comando, URLs, autenticação, fluxo de emissão, **decisões técnicas**, **garantia de produto existente**, **resiliência (SQS/Produto fora)**, testes, CI, troubleshooting.
- ADRs em `docs/adr/NNN-titulo.md` (Contexto · Decisão · Alternativas · Consequências). Existentes:
  - ADR-001 Monorepo com POM agregador
  - ADR-002 Arquitetura hexagonal enxuta
  - ADR-003 Estratégia de cache (cache-aside, TTL, cache negativo)
  - ADR-004 Autenticação JWT
  - ADR-005 Transactional Outbox
  - ADR-006 Retry, DLQ e idempotência
  - ADR-007 Proteção de dados de cartão (PAN) e LGPD
  - ADR-008 Frontend React com Nginx como reverse proxy (token em memória)
  - ADR-009 Revisão de segurança (OWASP Top 10), com a atualização de 21/09 (limite de login)
- Postman: `docs/postman/` com collection + environment (login salva token automaticamente em variável).

---

## 13. Forma de trabalho do Claude

1. **Entender** a issue → listar critérios de aceite e dúvidas.
2. **Perguntar** o que for estratégico (máx. 3 perguntas por vez, com opções e recomendação).
3. **Planejar** em passos pequenos; mostrar o plano antes de editar muitos arquivos.
4. **Implementar** com testes (TDD sempre que viável: teste → código → refatorar).
5. **Validar** (`./mvnw verify`, lint, build).
6. **Revisar** (seção 8).
7. **Entregar** o bloco "Pronto para commit" (seção 5) e **parar**.
8. Sugerir a **próxima issue** do roadmap.

Sempre trazer a opção **mais segura** como recomendação padrão e explicar trade-offs em poucas linhas.

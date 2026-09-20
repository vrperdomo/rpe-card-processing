# ADR-004 — Autenticação JWT

## Contexto

Os três microsserviços expõem endpoints de negócio que precisam de autenticação (PO-02, PR-*,
CA-*). O enunciado do desafio pede JWT, mas não especifica onde ele é emitido nem como os demais
serviços o validam sem duplicar um cadastro de usuários por serviço.

Não há, nesta fase, cadastro de usuários da plataforma (login de portador, operador etc.) — apenas
um usuário técnico único, usado para autenticar chamadas administrativas e testes ponta a ponta
(Postman, Swagger, frontend). Persistir esse usuário em banco, com repositório e hashing por
registro, seria desproporcional ao problema real: um único par usuário/senha, fixo, documentado.

## Decisão

**O Portador emite o JWT; Produto e Cartão são resource servers que apenas validam o mesmo
token** (decisão 19.2 do PRD):

- **Emissão:** `POST /api/v1/auth/login` no Portador Service recebe `username`/`password`,
  confere contra um **usuário técnico seed** (`rpe.auth.usuario-seed`, configurável via
  `AUTH_SEED_USERNAME`/`AUTH_SEED_PASSWORD_HASH`) com senha em **BCrypt**, e devolve um JWT
  assinado com HMAC-SHA256 (`iss`, `aud`, `sub`, `iat`, `exp`).
- **Segredo compartilhado:** os três serviços leem o mesmo segredo via `JWT_SECRET` (variável de
  ambiente, nunca hardcoded em produção). `iss` (`JWT_ISSUER`) e `aud` (`JWT_AUDIENCE`) também são
  compartilhados e validados por todos.
- **Validação:** cada serviço (incluindo o próprio Portador, para seus endpoints de negócio) é um
  `OAuth2 Resource Server` com `NimbusJwtDecoder`, validando assinatura, `iss` e `aud` via
  `DelegatingOAuth2TokenValidator`.
- **Expiração curta:** `rpe.jwt.expiracao` (padrão 30 min) — token vive só em memória no frontend
  (decisão 19.5); expirado, o usuário faz login de novo. Sem refresh token nesta fase.
- **Endpoints públicos:** somente `/api/v1/auth/login`, `/actuator/health/**`,
  `/v3/api-docs/**` e `/swagger-ui/**` (CLAUDE.md seção 6.7); todo o resto exige `Authorization:
  Bearer <token>`.
- **Erros de autenticação/autorização** retornam `ProblemDetail` (RFC 9457) via
  `JwtAuthEntryPoint` (401) e `JwtAccessDeniedHandler` (403) — nunca a página de erro padrão do
  Spring Security.

## Alternativas consideradas

1. **Cada serviço emite e valida seu próprio token.** Rejeitada: quebra o requisito de um único
   login para toda a jornada (cadastrar portador → consultar cartão) e obrigaria o frontend a
   gerenciar três tokens.
2. **Cadastro completo de usuários (tabela, repositório, endpoint de registro) já nesta fase.**
   Rejeitada por desproporção: o desafio não pede multiusuário: um usuário técnico seed, com senha
   em BCrypt e documentado, cobre o requisito PO-01 sem introduzir escopo não pedido (CLAUDE.md
   seção 0.5). Evolução natural, se necessário: repositório de usuários + endpoint de cadastro.
3. **OAuth2/OIDC completo com Authorization Server (Keycloak, Spring Authorization Server).**
   Rejeitada: custo de infraestrutura e operação incompatível com o prazo e com a demo de custo
   zero (decisão 19.8); JWT HS256 simétrico atende ao requisito com o mínimo de peças móveis.
4. **Refresh token.** Adiado: expiração curta + novo login é suficiente para o escopo avaliado;
   registrado como evolução junto com o BFF/cookie HttpOnly da ADR-008.

## Consequências

- Positivo: um único login autentica toda a jornada; Produto e Cartão não precisam conhecer
  credenciais, só o segredo de validação.
- Positivo: `AutenticarUseCase` depende só de portas (`GeradorToken`, `VerificadorSenha`),
  mantendo a camada de aplicação livre de tipos do Spring Security — comprovado em teste unitário
  com dublês, sem subir contexto Spring.
- Positivo: mesma configuração de resource server (`SecurityConfig`) já usada no Produto é
  replicada no Portador para seus próprios endpoints, mantendo o comportamento de erro (401/403 em
  `ProblemDetail`) idêntico entre serviços.
- Custo: segredo simétrico compartilhado entre três serviços — comprometer um serviço compromete
  os três. Aceitável para o escopo do desafio; em produção real, migrar para RS256 com chave
  pública distribuída (só o Portador teria a chave privada) é a evolução natural.
- Custo: usuário seed único, sem cadastro nem papéis/escopos — não há hoje diferenciação de
  permissão entre "admin" e "portador autenticado". Registrado como fora do escopo desta fase.

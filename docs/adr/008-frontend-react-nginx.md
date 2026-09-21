# ADR-008 — Frontend React servido por Nginx como proxy reverso (token em memória)

## Contexto

O desafio é de back-end; o frontend é um diferencial que precisa **demonstrar** o fluxo já
entregue (login → cadastro de portador → acompanhamento da emissão assíncrona do cartão) sem
ameaçar o que o avaliador prioriza: `docker compose up` em um comando, segurança e resiliência.

Três decisões aparecem juntas porque se condicionam:

1. Como o browser chega a **três** serviços com portas diferentes (8081/8082/8083).
2. Onde guardar o JWT emitido pelo Portador.
3. Como manter a demo de um comando, sem hospedagem paga nem etapa de build manual.

## Decisão

- **SPA React 19 + Vite + TypeScript estrito + Tailwind**, com TanStack Query (estado de
  servidor/polling), React Hook Form + Zod (formulários; entram junto das telas, #55/#56) e Vitest +
  Testing Library. Dependências
  fixadas em versão exata no `package.json` e `package-lock.json` commitado (`npm ci` no CI e no
  Docker). TypeScript fica na 6.0.x porque o `typescript-eslint` 8.70 só aceita `<6.1.0`; subir para
  a 7 exige esperar o lint acompanhar.
- **Nginx como proxy reverso e única origem do browser** (`frontend/nginx.conf`): serve o build
  estático e encaminha `/api/v1/auth` e `/api/v1/portadores` → Portador, `/api/v1/produtos` →
  Produto, `/api/v1/cartoes` → Cartão. O código do front usa só caminhos relativos. Em desenvolvimento
  (`npm run dev`) o Vite reproduz o mesmo mapa (`vite.config.ts`).
- **Token JWT só em memória** (variável do módulo de autenticação): nunca em `localStorage`,
  `sessionStorage` nem cookie. Recarregar a página exige novo login — custo aceito (o token vive 30
  min e não há refresh token, ver ADR-004).
- **Imagem multi-stage**: build com Node 22, runtime `nginxinc/nginx-unprivileged` (usuário
  não-root, porta 8080) com `HEALTHCHECK`. O serviço `frontend` entra no `docker-compose.yml` na
  porta `3000` (`FRONTEND_PORT`) e espera os três backends ficarem saudáveis.

### Consequências práticas do proxy

- **Sem CORS.** Como o browser só fala com uma origem, nenhum backend recebe chamada cross-origin e
  nenhum deles precisa de `CorsConfiguration`: o padrão do Spring (negar) já é o desejado. Isso
  fecha a lacuna 4 do ADR-009 ("CORS documentado, não implementado") pelo caminho mais seguro, em
  vez de abrir uma lista de origens permitidas nos serviços.
- **Cabeçalhos de segurança na borda**: `Content-Security-Policy` restritiva (`default-src 'self'`,
  sem `unsafe-inline`), `X-Content-Type-Options`, `X-Frame-Options: DENY`, `Referrer-Policy`,
  `Permissions-Policy`, `server_tokens off`. Não há HSTS porque o ambiente local não tem TLS.
- **IP real e o limite de login (ADR-009).** O Nginx **sobrescreve** `X-Forwarded-For` com o
  `$remote_addr` (não usa `$proxy_add_x_forwarded_for`): sendo a borda, ele não pode repassar o
  cabeçalho que o cliente mandou, senão bastaria forjá-lo a cada tentativa para escapar do limite.
  Limitação local: pela porta publicada do Docker todos os clientes chegam com o IP do gateway da
  rede (ex.: `172.21.0.1`), então em Compose o limite por origem se comporta como um limite único
  da máquina. Como só tentativas sem sucesso contam e login correto zera a contagem, o efeito
  prático numa demo é nulo; com um proxy/balanceador real na frente o IP do cliente é preservado.
- **Backend fora não derruba a UI.** O nome dos serviços fica em variável com `resolver 127.0.0.11`,
  então o Nginx resolve o DNS a cada requisição: sobe e continua de pé mesmo com um backend parado.
  Falha de conexão/timeout vira `503` no formato `application/problem+json`, o mesmo que a UI já
  trata; um 503 devolvido pelo próprio serviço (com `Retry-After`) passa intacto.

## Alternativas consideradas

1. **CORS nos três serviços (browser chamando 8081/8082/8083 direto).** Rejeitada: espalha uma lista
   de origens permitidas por três serviços, expõe as três portas ao browser e torna cada serviço
   responsável por uma política que o proxy resolve uma vez só. Também obrigaria o front a conhecer
   URLs diferentes por ambiente.
2. **API Gateway (Spring Cloud Gateway) como quarto serviço Java.** Rejeitada: mais uma JVM, mais
   memória e um componente a mais a testar e defender, para resolver só roteamento estático que o
   Nginx faz com quatro linhas.
3. **Token em `localStorage`/`sessionStorage` ou cookie.** Rejeitada: `localStorage` é lido por
   qualquer script injetado (XSS); cookie exigiria proteção CSRF e mudaria o modelo de autenticação
   do Portador. Em memória, um XSS ainda consegue usar o token enquanto a página vive, mas não o
   leva embora nem o reaproveita depois; a CSP restritiva reduz a superfície do XSS.
4. **`create-react-app`/Next.js.** Rejeitada: CRA está descontinuado; Next.js traria um servidor Node
   em runtime para uma aplicação que é só estática atrás de um proxy.
5. **Hospedar o front separado (Vercel/Netlify/Codespaces).** Rejeitada: quebra "um comando" e o
   critério de custo zero da demo (decisão de 20/09, CLAUDE.md seção 3.1).

## Consequências

- Positivo: a demo continua `docker compose up --build`, agora com a UI em `http://localhost:3000`.
- Positivo: superfície de ataque menor (uma origem, CSP restritiva, sem CORS, sem token persistido).
- Positivo: a UI degrada com os backends: fora do ar aparece como 503 tratado, não como tela quebrada.
- Custo: recarregar a página desloga (sem refresh token). Aceito para o escopo do desafio.
- Custo: o mapa de rotas existe em dois lugares (`nginx.conf` e `vite.config.ts`); um serviço novo
  precisa ser adicionado nos dois. Mantidos lado a lado e comentados para não divergirem.
- Fora do escopo desta entrega: painel de saúde/resiliência (#57) e testes E2E com Playwright (#58).

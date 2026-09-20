# ADR-001 — Monorepo com POM agregador

## Contexto

O desafio pede três microsserviços (Produto, Portador, Cartão) que evoluem juntos durante toda a
janela de desenvolvimento (17–21/09), compartilham decisões de infraestrutura (mesmo segredo JWT,
mesmo formato de evento, mesmo `docker-compose.yml`) e são avaliados como um sistema único, não
como três entregas independentes. Era preciso decidir, antes de escrever a primeira linha de
código, como versionar e buildar os três.

## Decisão

**Um único repositório Git (`rpe-card-processing`) com um POM Maven agregador na raiz**
(`packaging=pom`, `<modules>` apontando para `services/produto-service`,
`services/portador-service`, `services/cartao-service`), cada serviço com seu próprio artefato,
`Dockerfile` e ciclo de release dentro do mesmo `docker-compose.yml`.

- **Versões de dependência fixadas uma vez** no `dependencyManagement` do POM raiz (Spring Boot
  3.5.16 como parent, MapStruct, Resilience4j, WireMock, Awaitility, ArchUnit) — os três serviços
  herdam a mesma versão sem repetir número em três lugares.
- **`./mvnw verify` na raiz builda e testa os três serviços em sequência**; `./mvnw -pl
  services/cartao-service verify` builda um serviço isolado quando o trabalho é local a ele — o
  monorepo não obriga rebuild de tudo a cada mudança.
- **Um PR = uma issue = uma mudança tipicamente contida em um serviço**, mas o histórico de commits
  e a esteira de CI (`ci-backend` com matriz por serviço via `dorny/paths-filter`) enxergam os três
  como parte da mesma linha do tempo — essencial quando uma mudança é inerentemente cross-service
  (ex.: o formato do evento `CartaoEmissaoSolicitada`, que Portador publica e Cartão consome, PR
  #83/#84 desta entrega).
- **Um único `docker-compose.yml` na raiz** sobe os três serviços mais a infraestrutura
  (Postgres, Redis, LocalStack) com `docker compose up --build`, o que exige que os três estejam no
  mesmo repositório para o comando fazer sentido como "1 comando" (CLAUDE.md seção 1).

## Alternativas consideradas

1. **Três repositórios separados (`produto-service`, `portador-service`, `cartao-service`), cada um
   com seu próprio POM independente.** Rejeitada: exigiria versionar e publicar artefatos
   intermediários (ex.: um JAR de DTOs compartilhados) só para o Portador saber o formato de
   resposta do Cartão, ou duplicar esse conhecimento em cada repositório — overhead desproporcional
   para um desafio de prazo fixo em que os três serviços nunca são versionados/liberados de forma
   independente. Também obrigaria três `docker-compose.yml` distintos ou um quarto repositório só
   de orquestração.
2. **Monorepo sem POM agregador (cada serviço com POM raiz próprio, sem módulo pai comum).**
   Rejeitada: perde o `dependencyManagement` único — cada serviço fixaria a versão do Resilience4j
   ou do MapStruct de forma independente, com risco real de divergência silenciosa entre serviços
   que precisam se comportar de forma idêntica (ex.: os três validam o mesmo JWT com a mesma
   biblioteca).
3. **Monorepo com Gradle multi-módulo (`settings.gradle` + subprojects).** Rejeitada por preferência
   de ferramental: Maven é mais direto para builds simples e três módulos sem necessidade de build
   customizado (não há geração de código complexa nem múltiplas variantes de build que justifiquem
   Gradle); a escolha não é sobre superioridade técnica, é sobre familiaridade e velocidade dentro
   do prazo do desafio.

## Consequências

- Positivo: um `git clone` + `docker compose up --build` sobe o sistema inteiro — exatamente o
  critério "Docker em um comando" da seção 1 do CLAUDE.md.
- Positivo: mudança cross-service (evento novo, contrato HTTP novo) fica visível em um único PR
  quando faz sentido, ou em PRs sequenciais no mesmo histórico quando não faz — sem coordenação
  entre repositórios.
- Positivo: `ci-backend` roda com `paths-filter` por serviço, então um PR que só toca
  `cartao-service` não paga o custo de rebuildar Produto e Portador — o monorepo não implica CI
  lento.
- Custo: os três serviços compartilham a mesma versão do Spring Boot/parent — não há como um
  serviço ficar numa versão mais nova isoladamente sem subir a versão do POM raiz (e,
  consequentemente, reexecutar CI nos três). Aceitável: não há requisito de versionamento
  independente entre eles neste desafio.
- Custo: o histórico de commits mistura os três serviços — mitigado pelo prefixo de escopo
  obrigatório em Conventional Commits (`feat(cartao): ...`, CLAUDE.md seção 5), que mantém o
  histórico navegável por serviço mesmo em repositório único.

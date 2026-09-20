# ADR-002 — Arquitetura hexagonal enxuta

## Contexto

Os três serviços têm perfis de I/O diferentes, mas todos misturam regra de negócio com pelo menos
duas integrações externas: Produto tem JPA + publica evento SQS; Cartão tem JPA + Redis + client
HTTP + consumer SQS; Portador tem JPA + client HTTP + outbox + emissão de JWT. Sem uma fronteira
clara, é fácil a regra de negócio (ex.: "um cartão por par portador+produto", "portador precisa ter
18 anos") acabar espalhada em controllers ou em métodos de entidade JPA, dificultando testar a
regra sem subir banco/fila.

O enunciado do desafio também lista "SOLID/Clean Code" como primeiro critério de avaliação
(CLAUDE.md seção 1) — a estrutura de pacotes é, em si, parte do que é avaliado.

## Decisão

**Arquitetura hexagonal enxuta, idêntica nos três serviços** (CLAUDE.md seção 6.1):

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

- **`domain` é Java puro**: `Cartao`, `Produto`, `Portador` e seus Value Objects (`Pan`, `Cpf`,
  `Validade`) não importam `org.springframework.*`, `jakarta.persistence.*` nem
  `com.fasterxml.jackson.*` — a regra "um cartão por par portador+produto" ou "portador precisa ter
  18 anos" vive em métodos de domínio testáveis com JUnit puro, sem contexto Spring.
- **`application` orquestra, não implementa integração**: cada caso de uso (`EmitirCartaoUseCase`,
  `CadastrarPortadorUseCase`) depende só de `domain` e de **portas** (interfaces em
  `application/port/out`, ex.: `ProdutoClient`, `CartaoRepositorio`) — nunca de uma classe concreta
  de adapter. `@ConfigurationProperties` (pacote `config`) é a única exceção tolerada: são records
  de configuração tipada, sem comportamento de framework, então não violam o espírito da regra
  mesmo residindo fora de `domain`.
- **Adapters não se chamam entre si**: um controller (`adapters/in/web`) nunca importa uma entidade
  JPA (`adapters/out/persistence`) ou um DTO HTTP de saída diretamente — a única via é através da
  `application`.
- **Entidades JPA são sempre separadas do domínio** (`CartaoEntity` ≠ `Cartao`), convertidas nos
  adapters de persistência — nunca `@Entity` numa classe de `domain`.

**Aplicado nos três serviços de forma idêntica**, verificado agora por **ArchUnit** (um teste
`ArquiteturaTest` por serviço, cortado da entrega original de 21/09 e implementado nesta rodada de
backlog pós-release — CLAUDE.md seção 3.1): `domainNaoDependeDeFrameworksOuDeOutrasCamadas`,
`applicationNaoDependeDeAdapters`, `adaptersDeEntradaNaoDependemDeAdaptersDeSaida`,
`adaptersDeSaidaNaoDependemDeAdaptersDeEntrada` e `classesDeDominioSaoLivresDeAnotacoesJpa` rodam
como parte de `./mvnw test` e já passavam sem nenhuma mudança de código de produção — comprovando
que a convenção foi seguida manualmente em todo o histórico de PRs anterior a este ADR.

## Alternativas consideradas

1. **Camadas técnicas tradicionais (`controller/`, `service/`, `repository/`, `model/`).**
   Rejeitada: mistura regra de negócio com anotações JPA na mesma classe (`@Entity` com métodos de
   negócio) e não deixa explícita a fronteira entre "porta" (o que o caso de uso precisa) e
   "adapter" (como isso é implementado) — mais difícil de testar um caso de uso sem subir Spring.
2. **Hexagonal "completa" com módulos Maven separados por camada** (`domain.jar`,
   `application.jar`, `adapters.jar` por serviço). Rejeitada por desproporção: adicionaria 9+
   módulos Maven extras (3 por serviço × 3 serviços) só para reforçar uma fronteira que pacotes já
   bastam para comunicar e que o ArchUnit já verifica automaticamente — overhead de build sem
   ganho real de isolamento dentro do prazo do desafio.
3. **Sem verificação automatizada da fronteira (só revisão manual de código a cada PR).** Era a
   decisão original registrada no corte de escopo de 20/09 (CLAUDE.md seção 3.1, "ArchUnit... a
   arquitetura já é validada manualmente pela revisão de código a cada PR") — aceitável para
   destravar o release de `v1.0.0` dentro do prazo, mas deixa a regra sem rede de segurança contra
   regressão futura. Revertida nesta rodada de backlog: o custo de implementar era baixo (~3h) e o
   ArchUnit comprovou, ao rodar pela primeira vez, que nenhuma violação existia — validando que a
   disciplina manual funcionou, mas agora com garantia automatizada daqui para frente.

## Consequências

- Positivo: qualquer regra de negócio nova é, por construção, testável sem Testcontainers — só
  `domain`/`application` com dublês de porta, mantendo a suíte rápida (CLAUDE.md seção 7: unitário
  em `domain`/`application` é sempre obrigatório).
- Positivo: a partir de agora, um PR que quebrar a fronteira (ex.: um controller importando uma
  entidade JPA) falha o `./mvnw verify` imediatamente, em vez de depender de o revisor notar.
- Custo: mais arquivos pequenos por funcionalidade (porta + adapter + caso de uso, em vez de um
  único service technical) — aceito conscientemente como o preço de manter `domain`/`application`
  livres de infraestrutura, e mitigado por classes curtas (CLAUDE.md seção 6.2, métodos ≤ ~20
  linhas).
- Custo: a exceção de `application` poder depender de `config` (records `@ConfigurationProperties`)
  é uma flexibilização deliberada da regra estrita "application depende só de domain" — registrada
  aqui explicitamente para não ser reaberta como "violação" numa revisão futura sem contexto.

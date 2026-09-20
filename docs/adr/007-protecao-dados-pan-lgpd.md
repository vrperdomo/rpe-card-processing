# ADR-007 — Proteção de dados de cartão (PAN) e LGPD

## Contexto

O Cartão Service persiste o número completo do cartão (PAN — Primary Account Number) para poder
reconstituí-lo em consultas futuras (ex.: emissão de segunda via, exibição mascarada ao portador).
O CLAUDE.md (seções 0.4 e 6.7) proíbe explicitamente persistir PAN em claro e exige mascaramento
em toda resposta e log — requisito que também reflete a LGPD (minimização e proteção de dados
sensíveis) e as práticas de mercado para dados de cartão (PCI DSS, fora do escopo formal do
desafio, mas usado aqui como referência de boas práticas).

Duas necessidades conflitam: (1) nunca guardar o PAN legível no banco; (2) precisar recuperá-lo
para reconstituir o agregado `Cartao` em memória (ex.: para gerar `Pan.mascarado()` nas respostas)
e para garantir que um mesmo número de cartão não seja emitido duas vezes.

## Decisão

- **Cifra reversível (AES-GCM) para reconstituição, hash irreversível (SHA-256 + pepper) para
  unicidade** — dois mecanismos com propósitos diferentes, nunca um substituindo o outro:
  - `pan_cifrado`: PAN cifrado com `Encryptors.stronger(senha, salt)` do Spring Security Crypto
    (AES em modo GCM internamente — confirmado por inspeção do bytecode da biblioteca, já que a
    API não expõe um método `Encryptors.gcm` nesta versão), codificado em Base64. Usa IV aleatório
    a cada chamada (confirmado em teste: duas cifras do mesmo PAN produzem `pan_cifrado`
    diferentes, mas ambas decifram para o valor original) — protege contra análise de padrões
    mesmo com a chave comprometida.
  - `pan_hash`: `SHA-256(pan + pepper)`, determinístico, usado **apenas** para a constraint de
    unicidade (`uk_cartao_pan_hash`) — permite detectar PAN duplicado sem decifrar todo o
    dataset a cada inserção.
  - `senha`, `salt` e `pepper` vêm de variáveis de ambiente (`PAN_CRIPTOGRAFIA_SENHA`,
    `PAN_CRIPTOGRAFIA_SALT`, `PAN_CRIPTOGRAFIA_PEPPER`), nunca hardcoded; valores de exemplo
    apenas em `.env.example`, claramente marcados como inseguros para produção.
- **Mascaramento no domínio, não na borda**: `Pan.mascarado()` (`**** **** **** {ultimos4}`) vive
  no Value Object `Pan`, então qualquer código que só tenha acesso ao PAN mascarado (ex.: DTOs de
  resposta) nunca chega perto do valor completo — não depende de disciplina do desenvolvedor em
  cada controller lembrar de mascarar.
- **Mapeamento cifra/decifra isolado em `CartaoEntityMapper`** (fronteira `adapters/out/persistence`),
  não em MapStruct: o mapeamento depende do bean `CriptografoPan` injetado, o que foge do estilo
  puramente declarativo dos outros mappers do monorepo — escrito à mão para manter essa
  responsabilidade explícita e testável isoladamente.
- **`ultimos4` também persistido em coluna própria** (derivado do PAN, não sensível por si só) para
  permitir exibição mascarada sem decifrar o PAN completo em fluxos que só precisam dos 4 últimos
  dígitos.

## Alternativas consideradas

1. **Hash apenas (sem cifra reversível), nunca reconstituir o PAN.** Rejeitada: o domínio precisa
   do PAN completo em memória para gerar o `Pan` (Value Object) e validar Luhn/dígitos ao
   reconstituir o agregado a partir do banco; um hash unidirecional não permite isso.
2. **Cifra determinística (mesmo IV sempre) para servir também como chave de unicidade, dispensando
   o hash separado.** Rejeitada: cifra determinística vaza padrões (mesmo PAN sempre produz o
   mesmo texto cifrado, facilitando ataques de correlação) — exatamente o que o IV aleatório do
   AES-GCM evita. Separar cifra (com IV aleatório) de hash (determinístico, com pepper) mantém cada
   mecanismo otimizado para seu propósito.
3. **Tokenização via serviço externo (vault/HSM).** Rejeitada para este desafio: adiciona uma
   dependência de infraestrutura paga ou complexa, incompatível com a restrição de custo zero e
   Docker Compose em um comando (CLAUDE.md seção 3, decisões já tomadas).

## Consequências

- Positivo: PAN em claro nunca chega ao banco, a um log ou a uma resposta HTTP — comprovado por
  teste de integração (`CartaoRepositorioJpaAdapterIT.deveCifrarOPanAntesDePersistir`) que verifica
  que `pan_cifrado` não contém o PAN original.
- Positivo: a constraint de unicidade (`uk_cartao_pan_hash`) funciona sem decifrar nenhum registro
  existente — custo de verificação O(1) via índice único, não O(n) com decifra em memória.
- Custo: perda da chave (`senha`/`salt`) torna todo PAN cifrado irrecuperável — mitigação (rotação
  de chave, backup seguro do segredo) fica fora do escopo deste desafio, mas documentada aqui como
  risco operacional conhecido.
- Custo: toda leitura de `Cartao` a partir do banco paga o custo de uma decifra AES — aceitável na
  escala do desafio; não é um caminho de alta frequência (consulta pontual de cartão, não listagem
  em massa).

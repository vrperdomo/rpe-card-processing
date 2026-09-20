-- Idempotencia do consumer de CartaoEmissaoSolicitada (CA-02, CLAUDE.md secao 6.4): garante que
-- redelivery do SQS (at-least-once) nao emite o mesmo cartao duas vezes, na mesma transacao que
-- grava o cartao.
CREATE TABLE mensagem_processada (
    event_id UUID PRIMARY KEY,
    processado_em TIMESTAMPTZ NOT NULL
);

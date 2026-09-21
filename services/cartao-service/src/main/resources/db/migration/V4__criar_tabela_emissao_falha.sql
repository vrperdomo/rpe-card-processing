-- Última falha de emissão por portador (issue #117). O listener grava uma linha quando a mensagem
-- CartaoEmissaoSolicitada termina em falha definitiva (produto inexistente/cancelado, schema
-- incompatível) ou esgota as tentativas e o SQS a move para a DLQ. Uma linha por portador (PK):
-- uma nova falha do mesmo portador substitui a anterior, e uma emissão bem-sucedida a apaga.
-- criado_por segue a mesma regra de posse do cartão (ADR-009, A01).
CREATE TABLE emissao_falha (
    portador_id UUID PRIMARY KEY,
    produto_id UUID NOT NULL,
    motivo VARCHAR(255) NOT NULL,
    criado_por VARCHAR(255) NOT NULL,
    ocorrida_em TIMESTAMPTZ NOT NULL
);

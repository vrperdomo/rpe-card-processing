CREATE TABLE portador (
    id UUID PRIMARY KEY,
    nome VARCHAR(150) NOT NULL,
    cpf VARCHAR(11) NOT NULL,
    data_nascimento DATE NOT NULL,
    produto_id UUID NOT NULL,
    status VARCHAR(20) NOT NULL,
    versao BIGINT NOT NULL DEFAULT 0,
    criado_em TIMESTAMPTZ NOT NULL,
    atualizado_em TIMESTAMPTZ NOT NULL,
    CONSTRAINT uk_portador_cpf UNIQUE (cpf),
    CONSTRAINT ck_portador_status CHECK (status IN ('ATIVO', 'BLOQUEADO', 'CANCELADO'))
);

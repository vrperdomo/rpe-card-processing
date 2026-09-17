CREATE TABLE produto (
    id UUID PRIMARY KEY,
    nome VARCHAR(100) NOT NULL,
    descricao VARCHAR(255),
    categoria VARCHAR(30) NOT NULL,
    bin VARCHAR(6) NOT NULL,
    status VARCHAR(20) NOT NULL,
    versao BIGINT NOT NULL DEFAULT 0,
    criado_em TIMESTAMPTZ NOT NULL,
    atualizado_em TIMESTAMPTZ NOT NULL,
    CONSTRAINT uk_produto_nome UNIQUE (nome),
    CONSTRAINT ck_produto_status CHECK (status IN ('ATIVO', 'CANCELADO'))
);

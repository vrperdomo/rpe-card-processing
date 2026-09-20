CREATE TABLE cartao (
    id UUID PRIMARY KEY,
    portador_id UUID NOT NULL,
    produto_id UUID NOT NULL,
    pan_cifrado VARCHAR(255) NOT NULL,
    pan_hash VARCHAR(64) NOT NULL,
    ultimos4 VARCHAR(4) NOT NULL,
    nome_impresso VARCHAR(26) NOT NULL,
    validade VARCHAR(5) NOT NULL,
    status VARCHAR(20) NOT NULL,
    versao BIGINT NOT NULL DEFAULT 0,
    criado_em TIMESTAMPTZ NOT NULL,
    atualizado_em TIMESTAMPTZ NOT NULL,
    CONSTRAINT uk_cartao_pan_hash UNIQUE (pan_hash),
    CONSTRAINT uk_cartao_portador_produto UNIQUE (portador_id, produto_id),
    CONSTRAINT ck_cartao_status CHECK (status IN ('ATIVO', 'BLOQUEADO', 'CANCELADO'))
);

CREATE TABLE outbox_event (
    id UUID PRIMARY KEY,
    aggregate_type VARCHAR(50) NOT NULL,
    aggregate_id UUID NOT NULL,
    event_type VARCHAR(80) NOT NULL,
    payload JSONB NOT NULL,
    status VARCHAR(20) NOT NULL,
    tentativas INT NOT NULL DEFAULT 0,
    proxima_tentativa_em TIMESTAMPTZ,
    ultimo_erro TEXT,
    criado_em TIMESTAMPTZ NOT NULL,
    publicado_em TIMESTAMPTZ,
    CONSTRAINT ck_outbox_event_status CHECK (status IN ('PENDENTE', 'PUBLICADO', 'FALHOU'))
);

CREATE INDEX idx_outbox_pendente ON outbox_event (status, proxima_tentativa_em);

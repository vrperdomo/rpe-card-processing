-- ADR-009 (OWASP A01): dono do cartão = `sub` do usuário que cadastrou o portador, vindo no evento
-- CartaoEmissaoSolicitada (data.criadoPor). Linhas anteriores a esta migração recebem o dono
-- 'legado', que nenhum usuário real possui (falha fechada: ninguém as acessa). O DEFAULT existe só
-- para o backfill e é removido em seguida, para que todo INSERT novo informe o dono.
ALTER TABLE cartao ADD COLUMN criado_por VARCHAR(255) NOT NULL DEFAULT 'legado';
ALTER TABLE cartao ALTER COLUMN criado_por DROP DEFAULT;

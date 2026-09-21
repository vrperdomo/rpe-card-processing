-- ADR-009 (OWASP A01): dono do recurso = `sub` do JWT de quem cadastrou o portador.
-- Linhas anteriores a esta migração recebem o dono 'legado', que nenhum usuário real possui
-- (falha fechada: ninguém as acessa). O DEFAULT existe só para o backfill e é removido em
-- seguida, para que todo INSERT novo tenha de informar o dono explicitamente.
ALTER TABLE portador ADD COLUMN criado_por VARCHAR(255) NOT NULL DEFAULT 'legado';
ALTER TABLE portador ALTER COLUMN criado_por DROP DEFAULT;

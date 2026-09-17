-- Database-per-service (CLAUDE.md secao 2, PRD.md secao 7): cada microservico
-- possui seu proprio banco na mesma instancia Postgres, sem FK entre servicos.
CREATE DATABASE produto_db;
CREATE DATABASE portador_db;
CREATE DATABASE cartao_db;

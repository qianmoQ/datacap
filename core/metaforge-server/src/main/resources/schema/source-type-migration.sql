-- Idempotent source type upgrade for MySQL and H2.
-- Redis now ships as a single JDBC-backed connector named "Redis",
-- so sources created for the former "Redis Jdbc" plugin are re-pointed to it.
UPDATE metaforge_source SET _type = 'Redis' WHERE _type = 'Redis Jdbc';

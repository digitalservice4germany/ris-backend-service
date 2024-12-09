DELETE FROM incremental_migration.court_region WHERE court_id IN (SELECT id FROM incremental_migration.court);
DELETE FROM incremental_migration.court;

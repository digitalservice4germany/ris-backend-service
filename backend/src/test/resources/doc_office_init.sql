--- Test Dokstellen
INSERT INTO
    incremental_migration.documentation_office (id, abbreviation)
VALUES
    ('41e62dbc-e5b6-414f-91e2-0cfe559447d1', 'BGH'),
    ('f13c2fdb-5323-49aa-bc6d-09fa68c3acb9', 'CC-RIS'),
    ('ba90a851-3c54-4858-b4fa-7742ffbe8f05', 'DS') ON CONFLICT
    DO
    NOTHING;

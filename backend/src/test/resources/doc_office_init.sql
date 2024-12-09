--- Test Dokstellen
INSERT INTO
    incremental_migration.jurisdiction_type (id, label, juris_id)
VALUES
    ('27e099f9-5b47-4ce9-ac58-b84ca4643bc2', 'Ordentliche Gerichtsbarkeit', '1'),
    ('33ed4b4c-b4b8-44b1-a6bd-3144b5775f36', 'Sozialgerichtsbarkeit', '2'),
    ('4eb1e9e5-70e0-42d8-8348-98b1543b9455', 'Verfassungsgerichtsbarkeit', '3') ON CONFLICT
    DO
    NOTHING;

INSERT INTO
    incremental_migration.documentation_office (id, abbreviation, jurisdiction_type_id)
VALUES
    ('41e62dbc-e5b6-414f-91e2-0cfe559447d1', 'BGH', '27e099f9-5b47-4ce9-ac58-b84ca4643bc2'),
    ('f13c2fdb-5323-49aa-bc6d-09fa68c3acb9', 'CC-RIS', null),
    ('ba90a851-3c54-4858-b4fa-7742ffbe8f05', 'DS', null) ON CONFLICT
    DO
    NOTHING;

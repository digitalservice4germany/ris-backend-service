CREATE TABLE IF NOT EXISTS converted_document_element (
    id UUID PRIMARY KEY,
    documentation_unit_id UUID NOT NULL REFERENCES incremental_migration.documentation_unit (id) ON DELETE CASCADE,
    content TEXT,
    rank BIGINT NOT NULL
);

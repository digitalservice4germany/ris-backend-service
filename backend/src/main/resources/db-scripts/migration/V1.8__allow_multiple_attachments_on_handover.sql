CREATE TABLE IF NOT EXISTS handover_mail_attachments (
    id UUID NOT NULL DEFAULT gen_random_uuid (),
    handover_mail_id UUID NOT NULL,
    xml TEXT,
    file_name TEXT,
    PRIMARY KEY (id),
    CONSTRAINT fk_handover_mail
        FOREIGN KEY (handover_mail_id)
        REFERENCES handover_mail(id)
        ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_handover_mail_attachments_handover_mail_id
    ON handover_mail_attachments(handover_mail_id);

CREATE INDEX IF NOT EXISTS idx_handover_mail_attachments_file_name
    ON handover_mail_attachments(file_name);

-- Migrate data from handover_mail to handover_mail_attachments
-- Each row in handover_mail will move its 'xml' and 'file_name' columns to the new table
INSERT INTO handover_mail_attachments (handover_mail_id, xml, file_name)
SELECT id, xml, file_name
FROM handover_mail
WHERE xml IS NOT NULL OR file_name IS NOT NULL;

ALTER TABLE handover_mail
DROP COLUMN IF EXISTS xml,
DROP COLUMN IF EXISTS file_name;

ALTER TABLE handover_mail
    RENAME COLUMN documentation_unit_id TO entity_id;

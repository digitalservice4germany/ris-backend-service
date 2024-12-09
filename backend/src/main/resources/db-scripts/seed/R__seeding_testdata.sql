-- This timestamp updates the checksum, so flyway registers an update and migrates each time
-- ${flyway:timestamp}
delete from
  incremental_migration.documentation_unit
where
  document_number LIKE 'YY%';

-- Everytime the application starts, any test procedures that may have accumulated will be deleted
delete from
  incremental_migration.documentation_unit_procedure
where
  procedure_id IN (
    SELECT
      id
    from
      incremental_migration.procedure
    where
      name LIKE 'test_%'
  );

delete from
  incremental_migration.procedure
where
  name LIKE 'test_%';

INSERT INTO
  incremental_migration.documentation_unit (
    id,
    court_id,
    decision_date,
    document_number,
    document_type_id,
    documentation_office_id,
    guiding_principle,
    judicial_body,
    procedure,
    tenor,
    last_publication_date_time,
    scheduled_publication_date_time
  )
VALUES
  (
    gen_random_uuid (),
    (
      SELECT
        id
      FROM
        incremental_migration.court
      WHERE
      type
        = 'BAG'
    ),
    '1963-01-02',
    'YYTestDoc0001',
    (
      SELECT
        id
      FROM
        incremental_migration.document_type
      WHERE
        abbreviation = 'ÄN'
    ),
    (
      SELECT
        id
      FROM
        incremental_migration.documentation_office
      WHERE
        abbreviation = 'BAG'
    ),
    'guiding principle',
    '1.Senat, 2. Kammer',
    NULL,
    'tenor1',
    '2000-11-21 09:42:49.385920',
    NULL
  ),
  (
    gen_random_uuid (),
    NULL,
    '2024-02-02',
    'YYTestDoc0002',
    NULL,
    (
      SELECT
        id
      FROM
        incremental_migration.documentation_office
      WHERE
        abbreviation = 'DS'
    ),
    NULL,
    NULL,
    NULL,
    NULL,
    NULL,
    '2100-11-21 09:42:49.385920'
  ),
  (
    gen_random_uuid (),
    (
      SELECT
        id
      FROM
        incremental_migration.court
      WHERE
      type
        = 'BGH'
    ),
    '1964-10-10',
    'YYTestDoc0003',
    (
      SELECT
        id
      FROM
        incremental_migration.document_type
      WHERE
        abbreviation = 'Entscheidung'
    ),
    (
      SELECT
        id
      FROM
        incremental_migration.documentation_office
      WHERE
        abbreviation = 'DS'
    ),
    'guiding principle',
    '1.Senat, 2. Kammer',
    NULL,
    'tenorx',
    NULL,
    '2100-11-30 19:46:49.385920'
  ),
  (
    gen_random_uuid (),
    (
      SELECT
        id
      FROM
        incremental_migration.court
      WHERE
      type
        = 'AG'
        AND location = 'Bremen'
    ),
    '2022-02-02',
    'YYTestDoc0004',
    (
      SELECT
        id
      FROM
        incremental_migration.document_type
      WHERE
        abbreviation = 'Entscheidung'
    ),
    (
      SELECT
        id
      FROM
        incremental_migration.documentation_office
      WHERE
        abbreviation = 'DS'
    ),
    'guiding principle',
    '1.Senat, 2. Kammer',
    NULL,
    'tenorx',
    NULL,
    NULL
  ),
  (
    gen_random_uuid (),
    (
      SELECT
        id
      FROM
        incremental_migration.court
      WHERE
      type
        = 'BGH'
    ),
    NULL,
    'YYTestDoc0005',
    (
      SELECT
        id
      FROM
        incremental_migration.document_type
      WHERE
        abbreviation = 'Entscheidung'
    ),
    (
      SELECT
        id
      FROM
        incremental_migration.documentation_office
      WHERE
        abbreviation = 'DS'
    ),
    'guiding principle',
    '1.Senat, 2. Kammer',
    NULL,
    'tenorx',
    '2001-11-21 09:42:49.385920',
    NULL
  ),
  (
    gen_random_uuid (),
    NULL,
    '2024-01-02',
    'YYTestDoc0006',
    NULL,
    (
      SELECT
        id
      FROM
        incremental_migration.documentation_office
      WHERE
        abbreviation = 'DS'
    ),
    NULL,
    NULL,
    NULL,
    'tenorx',
    '2002-11-21 09:42:49.385920',
    NULL
  ),
  (
    gen_random_uuid (),
    NULL,
    '1935-12-02',
    'YYTestDoc0007',
    NULL,
    (
      SELECT
        id
      FROM
        incremental_migration.documentation_office
      WHERE
        abbreviation = 'DS'
    ),
    NULL,
    NULL,
    NULL,
    'tenorx',
    NULL,
    NULL
  ),
  (
    gen_random_uuid (),
    NULL,
    '1978-05-28',
    'YYTestDoc0008',
    (
      SELECT
        id
      FROM
        incremental_migration.document_type
      WHERE
        abbreviation = 'Entscheidung'
    ),
    (
      SELECT
        id
      FROM
        incremental_migration.documentation_office
      WHERE
        abbreviation = 'DS'
    ),
    'guiding principle',
    '1.Senat, 2. Kammer',
    NULL,
    'tenorx',
    NULL,
    NULL
  ),
  (
    gen_random_uuid (),
    NULL,
    '2022-10-01',
    'YYTestDoc0009',
    (
      SELECT
        id
      FROM
        incremental_migration.document_type
      WHERE
        abbreviation = 'BE'
    ),
    (
      SELECT
        id
      FROM
        incremental_migration.documentation_office
      WHERE
        abbreviation = 'DS'
    ),
    'guiding principle',
    '1.Senat, 2. Kammer',
    NULL,
    'tenorx',
    NULL,
    NULL
  ),
  (
    gen_random_uuid (),
    (
      SELECT
        id
      FROM
        incremental_migration.court
      WHERE
      type
        = 'BSG'
    ),
    '1990-02-03',
    'YYTestDoc0010',
    (
      SELECT
        id
      FROM
        incremental_migration.document_type
      WHERE
        abbreviation = 'BE'
    ),
    (
      SELECT
        id
      FROM
        incremental_migration.documentation_office
      WHERE
        abbreviation = 'DS'
    ),
    'guiding principle',
    '1.Senat, 2. Kammer',
    NULL,
    'tenorx',
    NULL,
    NULL
  ),
  (
    gen_random_uuid (),
    (
      SELECT
        id
      FROM
        incremental_migration.court
      WHERE
      type
        = 'BVerfG'
    ),
    '2022-02-03',
    'YYTestDoc0011',
    (
      SELECT
        id
      FROM
        incremental_migration.document_type
      WHERE
        abbreviation = 'BR'
    ),
    (
      SELECT
        id
      FROM
        incremental_migration.documentation_office
      WHERE
        abbreviation = 'DS'
    ),
    'guiding principle',
    '1.Senat, 2. Kammer',
    NULL,
    'tenorx',
    NULL,
    NULL
  ),
  (
    gen_random_uuid (),
    (
      SELECT
        id
      FROM
        incremental_migration.court
      WHERE
      type
        = 'BVerwG'
    ),
    '1987-09-09',
    'YYTestDoc0012',
    (
      SELECT
        id
      FROM
        incremental_migration.document_type
      WHERE
        abbreviation = 'EU'
    ),
    (
      SELECT
        id
      FROM
        incremental_migration.documentation_office
      WHERE
        abbreviation = 'DS'
    ),
    'guiding principle',
    '1.Senat, 2. Kammer',
    NULL,
    'tenorx',
    NULL,
    NULL
  ),
  (
    gen_random_uuid (),
    (
      SELECT
        id
      FROM
        incremental_migration.court
      WHERE
      type
        = 'BVerfG'
    ),
    '2080-02-02',
    'YYTestDoc0013',
    (
      SELECT
        id
      FROM
        incremental_migration.document_type
      WHERE
        abbreviation = 'GB'
    ),
    (
      SELECT
        id
      FROM
        incremental_migration.documentation_office
      WHERE
        abbreviation = 'DS'
    ),
    'guiding principle',
    '1.Senat, 2. Kammer',
    NULL,
    'tenorx',
    NULL,
    '2100-11-21 19:42:49.385920'
  ),
  (
    gen_random_uuid (),
    (
      SELECT
        id
      FROM
        incremental_migration.court
      WHERE
      type
        = 'AG'
        AND location = 'Aachen'
    ),
    '1989-01-01',
    'YYTestDoc0014',
    (
      SELECT
        id
      FROM
        incremental_migration.document_type
      WHERE
        abbreviation = 'BE'
    ),
    (
      SELECT
        id
      FROM
        incremental_migration.documentation_office
      WHERE
        abbreviation = 'DS'
    ),
    'guiding principle',
    '1.Senat, 2. Kammer',
    NULL,
    'tenorx',
    NULL,
    '2100-11-21 10:42:49.385920'
  ),
  (
    gen_random_uuid (),
    NULL,
    '2001-07-24',
    'YYTestDoc0015',
    (
      SELECT
        id
      FROM
        incremental_migration.document_type
      WHERE
        abbreviation = 'BE'
    ),
    (
      SELECT
        id
      FROM
        incremental_migration.documentation_office
      WHERE
        abbreviation = 'DS'
    ),
    'guiding principle',
    '1.Senat, 2. Kammer',
    NULL,
    'tenorx',
    '2005-11-21 09:42:49.385920',
    NULL
  );

INSERT INTO
  incremental_migration.status (
    id,
    created_at,
    publication_status,
    with_error,
    documentation_unit_id
  )
VALUES
  (
    gen_random_uuid (),
    '2024-03-14 18:38:43.043877 +00:00',
    'PUBLISHED',
    false,
    (
      SELECT
        id
      FROM
        incremental_migration.documentation_unit
      WHERE
        document_number = 'YYTestDoc0001'
    )
  ),
  (
    gen_random_uuid (),
    '2024-03-14 18:38:43.043877 +00:00',
    'PUBLISHED',
    true,
    (
      SELECT
        id
      FROM
        incremental_migration.documentation_unit
      WHERE
        document_number = 'YYTestDoc0002'
    )
  ),
  (
    gen_random_uuid (),
    '2024-03-14 18:38:43.043877 +00:00',
    'UNPUBLISHED',
    false,
    (
      SELECT
        id
      FROM
        incremental_migration.documentation_unit
      WHERE
        document_number = 'YYTestDoc0003'
    )
  ),
  (
    gen_random_uuid (),
    '2024-03-14 18:38:43.043877 +00:00',
    'PUBLISHED',
    false,
    (
      SELECT
        id
      FROM
        incremental_migration.documentation_unit
      WHERE
        document_number = 'YYTestDoc0004'
    )
  ),
  (
    gen_random_uuid (),
    '2024-03-14 18:38:43.043877 +00:00',
    'PUBLISHING',
    false,
    (
      SELECT
        id
      FROM
        incremental_migration.documentation_unit
      WHERE
        document_number = 'YYTestDoc0005'
    )
  ),
  (
    gen_random_uuid (),
    '2024-03-14 18:38:43.043877 +00:00',
    'UNPUBLISHED',
    false,
    (
      SELECT
        id
      FROM
        incremental_migration.documentation_unit
      WHERE
        document_number = 'YYTestDoc0006'
    )
  ),
  (
    gen_random_uuid (),
    '2024-03-14 18:38:43.043877 +00:00',
    'UNPUBLISHED',
    false,
    (
      SELECT
        id
      FROM
        incremental_migration.documentation_unit
      WHERE
        document_number = 'YYTestDoc0007'
    )
  ),
  (
    gen_random_uuid (),
    '2024-03-14 18:38:43.043877 +00:00',
    'UNPUBLISHED',
    false,
    (
      SELECT
        id
      FROM
        incremental_migration.documentation_unit
      WHERE
        document_number = 'YYTestDoc0008'
    )
  ),
  (
    gen_random_uuid (),
    '2024-03-14 18:38:43.043877 +00:00',
    'UNPUBLISHED',
    false,
    (
      SELECT
        id
      FROM
        incremental_migration.documentation_unit
      WHERE
        document_number = 'YYTestDoc0009'
    )
  ),
  (
    gen_random_uuid (),
    '2024-03-14 18:38:43.043877 +00:00',
    'PUBLISHING',
    false,
    (
      SELECT
        id
      FROM
        incremental_migration.documentation_unit
      WHERE
        document_number = 'YYTestDoc0010'
    )
  ),
  (
    gen_random_uuid (),
    '2024-03-14 18:38:43.043877 +00:00',
    'PUBLISHED',
    true,
    (
      SELECT
        id
      FROM
        incremental_migration.documentation_unit
      WHERE
        document_number = 'YYTestDoc0011'
    )
  ),
  (
    gen_random_uuid (),
    '2024-03-14 18:38:43.043877 +00:00',
    'PUBLISHED',
    false,
    (
      SELECT
        id
      FROM
        incremental_migration.documentation_unit
      WHERE
        document_number = 'YYTestDoc0012'
    )
  ),
  (
    gen_random_uuid (),
    '2024-03-14 18:38:43.043877 +00:00',
    'PUBLISHED',
    false,
    (
      SELECT
        id
      FROM
        incremental_migration.documentation_unit
      WHERE
        document_number = 'YYTestDoc0013'
    )
  ),
  (
    gen_random_uuid (),
    '2024-03-14 18:38:43.043877 +00:00',
    'PUBLISHED',
    true,
    (
      SELECT
        id
      FROM
        incremental_migration.documentation_unit
      WHERE
        document_number = 'YYTestDoc0014'
    )
  ),
  (
    gen_random_uuid (),
    '2024-03-13 18:38:43.043877 +00:00',
    'PUBLISHED',
    false,
    (
      SELECT
        id
      FROM
        incremental_migration.documentation_unit
      WHERE
        document_number = 'YYTestDoc0015'
    )
  ),
  (
    gen_random_uuid (),
    '2024-03-14 18:38:43.043877 +00:00',
    'UNPUBLISHED',
    false,
    (
      SELECT
          id
      FROM
          incremental_migration.documentation_unit
      WHERE
          document_number = 'YYTestDoc0015'
    )
  ),
  (
    gen_random_uuid (),
    '2024-03-12 18:38:43.043877 +00:00',
    'PUBLISHED',
    false,
    (
      SELECT
          id
      FROM
          incremental_migration.documentation_unit
      WHERE
          document_number = 'YYTestDoc0015'
    )
  )
    ;

UPDATE incremental_migration.documentation_unit SET current_status_id =
(SELECT id FROM incremental_migration.status WHERE documentation_unit_id = (SELECT id FROM incremental_migration.documentation_unit WHERE document_number = 'YYTestDoc0001'))
WHERE document_number = 'YYTestDoc0001';

UPDATE incremental_migration.documentation_unit SET current_status_id =
    (SELECT id FROM incremental_migration.status WHERE documentation_unit_id = (SELECT id FROM incremental_migration.documentation_unit WHERE document_number = 'YYTestDoc0002'))
WHERE document_number = 'YYTestDoc0002';


UPDATE incremental_migration.documentation_unit SET current_status_id =
    (SELECT id FROM incremental_migration.status WHERE documentation_unit_id = (SELECT id FROM incremental_migration.documentation_unit WHERE document_number = 'YYTestDoc0003'))
WHERE document_number = 'YYTestDoc0003';

UPDATE incremental_migration.documentation_unit SET current_status_id =
    (SELECT id FROM incremental_migration.status WHERE documentation_unit_id = (SELECT id FROM incremental_migration.documentation_unit WHERE document_number = 'YYTestDoc0004'))
WHERE document_number = 'YYTestDoc0004';

UPDATE incremental_migration.documentation_unit SET current_status_id =
    (SELECT id FROM incremental_migration.status WHERE documentation_unit_id = (SELECT id FROM incremental_migration.documentation_unit WHERE document_number = 'YYTestDoc0005'))
WHERE document_number = 'YYTestDoc0005';

UPDATE incremental_migration.documentation_unit SET current_status_id =
    (SELECT id FROM incremental_migration.status WHERE documentation_unit_id = (SELECT id FROM incremental_migration.documentation_unit WHERE document_number = 'YYTestDoc0006'))
WHERE document_number = 'YYTestDoc0006';

UPDATE incremental_migration.documentation_unit SET current_status_id =
    (SELECT id FROM incremental_migration.status WHERE documentation_unit_id = (SELECT id FROM incremental_migration.documentation_unit WHERE document_number = 'YYTestDoc0007'))
WHERE document_number = 'YYTestDoc0007';

UPDATE incremental_migration.documentation_unit SET current_status_id =
    (SELECT id FROM incremental_migration.status WHERE documentation_unit_id = (SELECT id FROM incremental_migration.documentation_unit WHERE document_number = 'YYTestDoc0008'))
WHERE document_number = 'YYTestDoc0008';

UPDATE incremental_migration.documentation_unit SET current_status_id =
    (SELECT id FROM incremental_migration.status WHERE documentation_unit_id = (SELECT id FROM incremental_migration.documentation_unit WHERE document_number = 'YYTestDoc0009'))
WHERE document_number = 'YYTestDoc0009';

UPDATE incremental_migration.documentation_unit SET current_status_id =
    (SELECT id FROM incremental_migration.status WHERE documentation_unit_id = (SELECT id FROM incremental_migration.documentation_unit WHERE document_number = 'YYTestDoc0010'))
WHERE document_number = 'YYTestDoc0010';

UPDATE incremental_migration.documentation_unit SET current_status_id =
    (SELECT id FROM incremental_migration.status WHERE documentation_unit_id = (SELECT id FROM incremental_migration.documentation_unit WHERE document_number = 'YYTestDoc0011'))
WHERE document_number = 'YYTestDoc0011';

UPDATE incremental_migration.documentation_unit SET current_status_id =
    (SELECT id FROM incremental_migration.status WHERE documentation_unit_id = (SELECT id FROM incremental_migration.documentation_unit WHERE document_number = 'YYTestDoc0012'))
WHERE document_number = 'YYTestDoc0012';

UPDATE incremental_migration.documentation_unit SET current_status_id =
    (SELECT id FROM incremental_migration.status WHERE documentation_unit_id = (SELECT id FROM incremental_migration.documentation_unit WHERE document_number = 'YYTestDoc0013'))
WHERE document_number = 'YYTestDoc0013';

UPDATE incremental_migration.documentation_unit SET current_status_id =
    (SELECT id FROM incremental_migration.status WHERE documentation_unit_id = (SELECT id FROM incremental_migration.documentation_unit WHERE document_number = 'YYTestDoc0014'))
WHERE document_number = 'YYTestDoc0014';

UPDATE incremental_migration.documentation_unit SET current_status_id =
    (SELECT id FROM incremental_migration.status WHERE documentation_unit_id = (SELECT id FROM incremental_migration.documentation_unit WHERE document_number = 'YYTestDoc0015') ORDER BY created_at DESC LIMIT 1)
WHERE document_number = 'YYTestDoc0015';


UPDATE
  incremental_migration.documentation_unit
SET
  note = 'dies ist eine test notiz'
WHERE
  incremental_migration.documentation_unit.document_number = 'YYTestDoc0015';

INSERT INTO
  incremental_migration.file_number (id, value, documentation_unit_id, rank)
VALUES
  (
    gen_random_uuid (),
    'fileNumber1',
    (
      SELECT
        id
      FROM
        incremental_migration.documentation_unit
      WHERE
        document_number = 'YYTestDoc0001'
    ),
    1
  ),
  (
    gen_random_uuid (),
    'fileNumber2',
    (
      SELECT
        id
      FROM
        incremental_migration.documentation_unit
      WHERE
        document_number = 'YYTestDoc0001'
    ),
    2
  ),
  (
    gen_random_uuid (),
    'fileNumber3',
    (
      SELECT
        id
      FROM
        incremental_migration.documentation_unit
      WHERE
        document_number = 'YYTestDoc0004'
    ),
    1
  ),
  (
    gen_random_uuid (),
    'fileNumber4',
    (
      SELECT
        id
      FROM
        incremental_migration.documentation_unit
      WHERE
        document_number = 'YYTestDoc0012'
    ),
    1
  ),
  (
    gen_random_uuid (),
    'fileNumber5',
    (
      SELECT
        id
      FROM
        incremental_migration.documentation_unit
      WHERE
        document_number = 'YYTestDoc0013'
    ),
    1
  );

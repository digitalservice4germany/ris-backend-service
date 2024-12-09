INSERT INTO
  incremental_migration.norm_abbreviation (
    id,
    abbreviation,
    decision_date,
    document_id,
    document_number,
    official_letter_abbreviation,
    official_short_title,
    official_long_title,
    source
  )
VALUES
  (
    '33333333-2222-3333-4444-555555555555',
    'norm abbreviation 1',
    '2021-01-01',
    1,
    'document number 1',
    'official letter abbreviation 1',
    'official short title 1',
    'official long title 1',
    'R'
  ),
  (
    '33333333-2222-3333-4444-666666666666',
    'norm abbreviation 2',
    '2021-01-01',
    2,
    'document number 2',
    'official letter abbreviation 1',
    'official short title 1',
    'official long title 1',
    'R'
  );

INSERT INTO
  incremental_migration.legal_force_type (id, abbreviation, label, juris_id)
VALUES
  (
    '11111111-2222-3333-4444-555555555555',
    'legal force type 1',
    'legal force type 1',
    1
  ),
  (
    '11111111-2222-3333-4444-666666666666',
    'legal force type 2',
    'legal force type 2',
    2
  ),
  (
    '11111111-2222-3333-4444-777777777777',
    'legal force type 3',
    'legal force type 3',
    3
  );

INSERT INTO
  incremental_migration.region (id, code, long_text, applicability)
VALUES
  (
    '55555555-2222-3333-4444-555555555555',
    'BY',
    'Bayern',
    true
  ),
  (
    '55555555-2222-3333-4444-666666666666',
    'CH',
    'Schweiz',
    false
  );

insert into
  incremental_migration.documentation_unit (id, document_number, documentation_office_id)
values
  (
    '46f9ae5c-ea72-46d8-864c-ce9dd7cee4a3',
    'documentnr001',
    'ba90a851-3c54-4858-b4fa-7742ffbe8f05'
  ),
  (
    'f13e7fe2-78a5-11ee-b962-0242ac120002',
    'documentnr002',
    'ba90a851-3c54-4858-b4fa-7742ffbe8f05'
  );

insert into
    incremental_migration.status (id, documentation_unit_id, publication_status, created_at, with_error)
values
    (
        '75988131-f355-414d-9da5-dcbcdbf4b98f',
        '46f9ae5c-ea72-46d8-864c-ce9dd7cee4a3',
        'PUBLISHED',
     current_timestamp,
        false
    ),
    (
        '65988131-f355-414d-9da5-dcbcdbf4b98f',
        'f13e7fe2-78a5-11ee-b962-0242ac120002',
        'PUBLISHED',
        current_timestamp,
        false
    );

update incremental_migration.documentation_unit set current_status_id = '75988131-f355-414d-9da5-dcbcdbf4b98f' where id = '46f9ae5c-ea72-46d8-864c-ce9dd7cee4a3';
update incremental_migration.documentation_unit set current_status_id = '65988131-f355-414d-9da5-dcbcdbf4b98f' where id = 'f13e7fe2-78a5-11ee-b962-0242ac120002';

insert into
  incremental_migration.norm_reference (
    id,
    norm_abbreviation_raw_value,
    single_norm,
    date_of_version,
    date_of_relevance,
    documentation_unit_id,
    norm_abbreviation_id,
    rank
  )
values
  (
    'f0232240-7416-11ee-b962-0242ac120001',
    '123',
    'single norm 1',
    '2011-01-21',
    '2011',
    '46f9ae5c-ea72-46d8-864c-ce9dd7cee4a3',
    '33333333-2222-3333-4444-555555555555',
    1
  ),
  (
    'f0232240-7416-11ee-b962-0242ac120002',
    '123',
    'single norm 1',
    '2011-01-21',
    '2011',
    'f13e7fe2-78a5-11ee-b962-0242ac120002',
    '33333333-2222-3333-4444-555555555555',
    1
  );

INSERT INTO
  incremental_migration.legal_force (
    id,
    legal_force_type_id,
    region_id,
    norm_reference_id
  )
VALUES
  (
    '33333333-2222-3333-6666-555555555555',
    '11111111-2222-3333-4444-555555555555',
    '55555555-2222-3333-4444-555555555555',
    'f0232240-7416-11ee-b962-0242ac120002'
  );

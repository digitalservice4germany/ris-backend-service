INSERT INTO
  incremental_migration.document_category (id, label)
VALUES
  ('dd315130-1fda-46d5-bccd-d587cf51c664', 'R');

INSERT INTO
  incremental_migration.citation_type (
    id,
    abbreviation,
    label,
    documentation_unit_document_category_id,
    citation_document_category_id,
    juris_id
  )
VALUES
  (
    '4e768071-1a19-43a1-8ab9-c185adec94bf',
    'Anwendung',
    'Anwendung',
    'dd315130-1fda-46d5-bccd-d587cf51c664',
    'dd315130-1fda-46d5-bccd-d587cf51c664',
    48
  ),
  (
    '6b4bd747-fce9-4e49-8af4-3fb4f1d3663c',
    'Nan',
    'Nichtanwendung',
    'dd315130-1fda-46d5-bccd-d587cf51c664',
    'dd315130-1fda-46d5-bccd-d587cf51c664',
    49
  );

insert into
  incremental_migration.court (
    id,
    juris_id,
    location,
    type
  )
values
  (
    '96301f85-9bd2-4690-a67f-f9fdfe725de3',
    '1357',
    'Karlsruhe',
    'BGH'
  );

insert into
  incremental_migration.document_type (id, abbreviation, label, document_category_id)
values
  (
    'b57e016e-665d-486c-902e-c191f5a7acf6',
    'Bes',
    'Beschluss',
    'dd315130-1fda-46d5-bccd-d587cf51c664'
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
        '95988131-f355-414d-9da5-dcbcdbf4b98f',
        '46f9ae5c-ea72-46d8-864c-ce9dd7cee4a3',
        'PUBLISHED',
        current_timestamp,
     false
    ),
    (
        '85988131-f355-414d-9da5-dcbcdbf4b98f',
        'f13e7fe2-78a5-11ee-b962-0242ac120002',
        'PUBLISHED',
        current_timestamp,
        false
    );

update incremental_migration.documentation_unit set current_status_id = '95988131-f355-414d-9da5-dcbcdbf4b98f' where id = '46f9ae5c-ea72-46d8-864c-ce9dd7cee4a3';
update incremental_migration.documentation_unit set current_status_id = '85988131-f355-414d-9da5-dcbcdbf4b98f' where id = 'f13e7fe2-78a5-11ee-b962-0242ac120002';

insert into
  incremental_migration.related_documentation (
    id,
    court_id,
    date,
    document_number,
    document_type_id,
    file_number,
    citation_type_id,
    dtype,
    documentation_unit_id,
    rank
  )
values
  (
    'f0232240-7416-11ee-b962-0242ac120002',
    '96301f85-9bd2-4690-a67f-f9fdfe725de3',
    '2011-01-21',
    null,
    'b57e016e-665d-486c-902e-c191f5a7acf6',
    'abc',
    '4e768071-1a19-43a1-8ab9-c185adec94bf',
    'caselaw_active_citation',
    '46f9ae5c-ea72-46d8-864c-ce9dd7cee4a3',
    1
  ),
  (
    'f0232240-7416-11ee-b962-0242ac120003',
    null,
    null,
    'documentnr002',
    null,
    null,
    '6b4bd747-fce9-4e49-8af4-3fb4f1d3663c',
    'caselaw_active_citation',
    '46f9ae5c-ea72-46d8-864c-ce9dd7cee4a3',
    2
  );

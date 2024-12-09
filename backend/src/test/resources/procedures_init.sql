
INSERT INTO
  incremental_migration.documentation_unit (id, document_number, documentation_office_id)
VALUES
  (
    '16f9ae5c-ea72-46d8-864c-ce9dd7cee4a3',
    '1234567890123',
    'ba90a851-3c54-4858-b4fa-7742ffbe8f05'
  ),
  (
    '26f9ae5c-ea72-46d8-864c-ce9dd7cee4a3',
    'docNumber00002',
    'ba90a851-3c54-4858-b4fa-7742ffbe8f05'
  ),
  (
    '36f9ae5c-ea72-46d8-864c-ce9dd7cee4a3',
    'docNumber00003',
    'ba90a851-3c54-4858-b4fa-7742ffbe8f05'
  ),
  (
    '46f9ae5c-ea72-46d8-864c-ce9dd7cee4a3',
    'bghDocument123',
    '41e62dbc-e5b6-414f-91e2-0cfe559447d1'
  );

insert into
    incremental_migration.status (id, documentation_unit_id, publication_status, created_at, with_error)
values
    (
        '75988131-f355-414d-9da5-dcbcdbf4b98f',
        '16f9ae5c-ea72-46d8-864c-ce9dd7cee4a3',
        'PUBLISHED',
        current_timestamp,
        false
    ),
    (
        '65988131-f355-414d-9da5-dcbcdbf4b98f',
        '26f9ae5c-ea72-46d8-864c-ce9dd7cee4a3',
        'PUBLISHED',
     current_timestamp,
        false
    ),
    (
        '55988131-f355-414d-9da5-dcbcdbf4b98f',
        '36f9ae5c-ea72-46d8-864c-ce9dd7cee4a3',
        'PUBLISHED',
        current_timestamp,
        false
    ),
    (
        '45988131-f355-414d-9da5-dcbcdbf4b98f',
        '46f9ae5c-ea72-46d8-864c-ce9dd7cee4a3',
        'PUBLISHED',
        current_timestamp,
        false
    );

update incremental_migration.documentation_unit set current_status_id = '75988131-f355-414d-9da5-dcbcdbf4b98f' where id = '16f9ae5c-ea72-46d8-864c-ce9dd7cee4a3';
update incremental_migration.documentation_unit set current_status_id = '65988131-f355-414d-9da5-dcbcdbf4b98f' where id = '26f9ae5c-ea72-46d8-864c-ce9dd7cee4a3';
update incremental_migration.documentation_unit set current_status_id = '55988131-f355-414d-9da5-dcbcdbf4b98f' where id = '36f9ae5c-ea72-46d8-864c-ce9dd7cee4a3';
update incremental_migration.documentation_unit set current_status_id = '45988131-f355-414d-9da5-dcbcdbf4b98f' where id = '46f9ae5c-ea72-46d8-864c-ce9dd7cee4a3';

INSERT INTO
  incremental_migration.procedure (id, documentation_office_id, name, created_at, documentation_office_user_group_id)
VALUES
  (
    '1e768071-1a19-43a1-8ab9-c185adec94bf',
    'ba90a851-3c54-4858-b4fa-7742ffbe8f05',
    'procedure1',
    null,
    null
  ),
  (
    '2e768071-1a19-43a1-8ab9-c185adec94bf',
    'ba90a851-3c54-4858-b4fa-7742ffbe8f05',
    'procedure2',
    null,
    null
  ),
  (
    '3e768071-1a19-43a1-8ab9-c185adec94bf',
    'ba90a851-3c54-4858-b4fa-7742ffbe8f05',
    'procedure3',
    null,
    null
  ),
  (
    '9e768071-1a19-43a1-8ab9-c185adec94bf',
    '41e62dbc-e5b6-414f-91e2-0cfe559447d1',
    'testProcedure BGH',
    null,
    null
  ),
  (
    '3f768071-1a19-43a1-8ab9-c185adec94bf',
    'ba90a851-3c54-4858-b4fa-7742ffbe8f05',
    'with date',
    '2023-12-04 18:21:48.334174+00',
    null
  ),
  (
    '4f768071-1a19-43a1-8ab9-c185adec94bf',
    'ba90a851-3c54-4858-b4fa-7742ffbe8f05',
    'with date in past',
    '2022-12-04 18:21:48.334174+00',
    null
  ),
  (
    '5f768071-1a19-43a1-8ab9-c185adec94bf',
    'ba90a851-3c54-4858-b4fa-7742ffbe8f05',
    'without date',
    null,
    null
  );

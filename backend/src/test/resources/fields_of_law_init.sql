insert into
  incremental_migration.field_of_law (id, identifier, text, juris_id, notation)
values
  (
    'ef382996-497f-4c2f-9a30-1c73d8ac0a87',
    'FL',
    'fl',
    1,
    'NEW'
  ),
  (
    '71defe05-cd4d-43e5-a07e-06c611b81a26',
    'FL-01',
    'fl-01',
    2,
    'NEW'
  ),
  (
    'ce5bab9c-8852-49a3-8ed8-08b67399abde',
    'FL-01-01',
    'multiple cats are allowed on a tree',
    3,
    'NEW'
  ),
  (
    'dfc6c738-7e81-40bd-8aa4-c5426605e9b0',
    'FO',
    'fo',
    4,
    'NEW'
  ),
  (
    '93393410-0ab0-48ab-a61d-5056e440174a',
    'FL-02',
    'fl-02',
    5,
    'NEW'
  ),
  (
    '64df49b7-1efe-4b18-96b3-46efef1ce21d',
    'FL-03',
    'fl-03',
    6,
    'NEW'
  ),
  (
    'cb6cb7f4-4ad3-43ec-ae5e-a32d6eaab09f',
    'FL-04',
    'fl-04',
    7,
    'NEW'
  ),
  (
    '6959af10-7355-4e22-858d-29a485189957',
    'AB-01',
    'Some text here',
    8,
    'NEW'
  ),
  (
    '155da15c-f419-4c3c-a8f7-344d8ef10eea',
    'AB-01-01',
    'More text also here',
    9,
    'NEW'
  ),
  (
    'ba67f3bd-ee06-4c04-8b14-72c615e23393',
    'CD',
    'Other text without more',
    10,
    'NEW'
  ),
  (
    'b4f9ee05-38ed-49c3-89d6-50141f031017',
    'CD-01',
    'Text means writing here',
    11,
    'NEW'
  ),
  (
    '64202894-ea0b-46d3-bc02-e8cf21248644',
    'CD-02',
    'Aber a word starting with ab and text + here',
    12,
    'NEW'
  );

insert into
  incremental_migration.field_of_law_norm (
    id,
    abbreviation,
    single_norm_description,
    field_of_law_id
  )
values
  (
    'bbf8d202-48b8-4420-85ce-33abf0b28f8f',
    'ABC',
    '§ 123',
    'ef382996-497f-4c2f-9a30-1c73d8ac0a87'
  ),
  (
    '28c22416-9cca-4a38-8059-8f6e9491af10',
    'DEF',
    '§ 456',
    '71defe05-cd4d-43e5-a07e-06c611b81a26'
  ),
  (
    '38c22416-9cca-4a38-8059-8f6e9491af10',
    'DEF',
    '§ 456',
    'ba67f3bd-ee06-4c04-8b14-72c615e23393'
  ),
  (
    'c13c0993-d93e-4e3c-bf55-2979a66f5c5d',
    'abcd',
    '§ 123',
    '6959af10-7355-4e22-858d-29a485189957'
  ),
  (
    '09e34de5-a6b5-417c-8aaa-8e362a4ea44c',
    'abxyz',
    '§ 123',
    '155da15c-f419-4c3c-a8f7-344d8ef10eea'
  ),
  (
    '8f757de8-9b39-40e3-ac29-746442f7b71b',
    'dab',
    '§ 012',
    'b4f9ee05-38ed-49c3-89d6-50141f031017'
  ),
  (
    'd1f395e3-345f-4e69-804a-34e5e13654b2',
    'aber hallo',
    '§ 123',
    '64202894-ea0b-46d3-bc02-e8cf21248644'
  );

insert into
  incremental_migration.field_of_law_field_of_law_parent (field_of_law_id, field_of_law_parent_id)
values
  (
    '71defe05-cd4d-43e5-a07e-06c611b81a26',
    'ef382996-497f-4c2f-9a30-1c73d8ac0a87'
  ),
  (
    'ce5bab9c-8852-49a3-8ed8-08b67399abde',
    '71defe05-cd4d-43e5-a07e-06c611b81a26'
  ),
  (
    '93393410-0ab0-48ab-a61d-5056e440174a',
    'ef382996-497f-4c2f-9a30-1c73d8ac0a87'
  ),
  (
    '64df49b7-1efe-4b18-96b3-46efef1ce21d',
    'ef382996-497f-4c2f-9a30-1c73d8ac0a87'
  ),
  (
    'cb6cb7f4-4ad3-43ec-ae5e-a32d6eaab09f',
    'ef382996-497f-4c2f-9a30-1c73d8ac0a87'
  ),
  (
    '155da15c-f419-4c3c-a8f7-344d8ef10eea',
    '6959af10-7355-4e22-858d-29a485189957'
  ),
  (
    'b4f9ee05-38ed-49c3-89d6-50141f031017',
    'ba67f3bd-ee06-4c04-8b14-72c615e23393'
  ),
  (
    '64202894-ea0b-46d3-bc02-e8cf21248644',
    'ba67f3bd-ee06-4c04-8b14-72c615e23393'
  );

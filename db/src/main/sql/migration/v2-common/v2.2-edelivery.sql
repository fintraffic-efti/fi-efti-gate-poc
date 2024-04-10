call create_classification('ed_message_type'::name);
call create_classification('ed_message_direction'::name);
call audit.activate('ed_message_type'::name);
call audit.activate('ed_message_direction'::name);

create table ed_message (
  id integer generated by default as identity primary key,
  timestamp timestamp with time zone not null default transaction_timestamp(),
  direction_id smallint references ed_message_direction (id),
  --type_id smallint references ed_message_type (id),
  content_type text,
  message_id text,
  conversation_id text,
  from_id text,
  to_id text,
  payload text
);

create sequence conversation_id_seq start 1;
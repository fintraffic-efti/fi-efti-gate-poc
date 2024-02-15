create or replace procedure create_classification(table_name name)
language plpgsql
as $$
begin
execute format(
'create table %I (
  id int primary key,
  label_en text, label_fi text, label_sv text,
  abbr_en text, abbr_fi text, abbr_sv text,
  valid boolean not null default true,
  ordinal int not null default 0,
  description text
)', table_name);
end
$$;

create or replace procedure create_classification_constant(table_name name, class_name name, id int)
language plpgsql
as $p$
begin
execute format(
'create or replace function %I$%I() returns int as $$
begin return %s; end; $$ language plpgsql immutable', table_name, class_name, id);
end
$p$;

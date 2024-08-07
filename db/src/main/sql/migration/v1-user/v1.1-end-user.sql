call create_classification('role'::name);

create table end_user (
  id int generated by default as identity primary key,
  role_id int default 0 not null references role (id),
  ssn text unique,
  email text unique,
  cognito_id text unique,
  login_time timestamp with time zone,

  organisation text,
  department text,
  first_name text,
  last_name text,
  name text,
  platform_url text,
  platform_id text unique
);

call audit.create_audit_table('end_user'::name, null);
alter table audit.end_user drop column login_time;

call audit.create_audit_procedure('end_user'::name);
call audit.create_audit_insert_trigger('end_user'::name, 'end_user'::name);
call audit.create_audit_update_trigger('end_user'::name, 'end_user'::name,
  audit.update_condition('end_user'::name));

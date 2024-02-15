
create or replace procedure create_db_user(username name, password text) language plpgsql
as $p$
begin
  if password = '' or password is null then
    execute format('raise exception ''Empty %I password''', username);
  end if;
  if not exists (select * from pg_user where usename = username) then
    execute format('create user %I', username);
  end if;
  execute format('alter user %I password ''%s''', username, password);
end
$p$;
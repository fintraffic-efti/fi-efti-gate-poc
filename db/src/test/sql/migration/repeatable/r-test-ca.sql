insert into end_user (id, role_id, name) values (-20, 1, 'Test CA')
on conflict (id) do update set
  role_id = excluded.role_id,
  name = excluded.name;
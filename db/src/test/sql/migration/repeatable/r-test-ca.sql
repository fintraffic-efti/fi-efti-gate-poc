insert into end_user (id, role_id, name, platform_id) values (-20, 1, 'Test CA', 'aap1')
on conflict (id) do update set
  role_id = excluded.role_id,
  name = excluded.name;

insert into end_user (id, role_id, name, email)
values
(0, -1, 'database', 'database@efti.fintraffic.fi'),
(-1, -1, 'authentication', 'authentication@efti.fintraffic.fi')
on conflict (id) do update set
  role_id = excluded.role_id,
  first_name = excluded.first_name,
  last_name = excluded.last_name,
  email = excluded.email;

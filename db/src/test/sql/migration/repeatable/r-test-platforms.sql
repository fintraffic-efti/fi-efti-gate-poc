set xxxx.client_id = 0;
set xxxx.client_service_uri = 'database.xxxx';

insert into end_user (id, role_id, name, platform_url) values
  (-10, 0, 'Test platform', 'http://localhost:9091/api/v1')
on conflict (id) do update set
  role_id = excluded.role_id,
  name = excluded.name,
  email = excluded.email,
  platform_url = excluded.platform_url;

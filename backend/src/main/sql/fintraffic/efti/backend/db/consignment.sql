
-- :name upsert-consignment! :!
insert into consignment (
  gate_url, platform_url, data_id, is_dangerous_goods,
  journey_start_time, journey_end_time,
  country_start_id, country_end_id)
values (
   :gate-url, :platform-url, :data-id, :is-dangerous-goods,
   :journey-start-time, :journey-end-time,
   :country-start-id, :country-end-id)
on conflict (gate_url, platform_url, data_id) do update set
  is_dangerous_goods = excluded.is_dangerous_goods,
  journey_start_time = excluded.journey_start_time,
  journey_end_time = excluded.journey_end_time,
  country_start_id = excluded.country_start_id,
  country_end_id = excluded.country_end_id;
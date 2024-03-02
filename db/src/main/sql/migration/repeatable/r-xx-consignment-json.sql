
create or replace view consignment_json as
select
  (select json_agg(vehicle) from transport_vehicle vehicle where vehicle.consignment_id = consignment.id) as transport_vehicles,
  consignment.*
from consignment
;

-- :name select-consignment
select consignment.*
from consignment_json consignment
where consignment.gate_url = :gate-url and
      consignment.platform_url = :platform-url and
      consignment.data_id = :data-id
;

-- :name select-consignments
select consignment.* from consignment_json consignment
where
  (:vehicle-id is null or exists (
    select from transport_vehicle vehicle
    where vehicle.consignment_id = consignment.id and vehicle.vehicle_id = :vehicle-id))
limit :limit offset :offset;
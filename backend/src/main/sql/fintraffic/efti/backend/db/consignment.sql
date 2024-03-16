
-- :name select-consignment
select consignment.*
from consignment_json consignment
where consignment.gate_id = :gate-id and
      consignment.platform_id = :platform-id::text and
      consignment.data_id = :data-id
;

-- :name select-consignments
select consignment.* from consignment_json consignment
where
  (:vehicle-id::int is null or exists (
    select from transport_vehicle vehicle
    where vehicle.consignment_id = consignment.id and vehicle.vehicle_id = :vehicle-id))
limit :limit offset :offset;
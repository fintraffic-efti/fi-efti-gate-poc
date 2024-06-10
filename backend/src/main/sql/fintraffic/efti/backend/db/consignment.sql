
-- :name select-consignment
select consignment.*
from consignment_json consignment
where consignment.uil$gate_id = :gate-id and
      consignment.uil$platform_id = :platform-id::text and
      consignment.uil$data_id = :data-id
;

-- :name select-consignments
select consignment.* from consignment_json consignment
where
  (:vehicle-id::text is null or exists (
    select from transport_vehicle vehicle
    where vehicle.consignment_id = consignment.id and vehicle.vehicle_id = :vehicle-id))
limit :limit offset :offset;
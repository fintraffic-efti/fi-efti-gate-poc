
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
  (:identifier::text is null or exists (
    select from transport_movement movement
    where movement.consignment_id = consignment.id and
          movement.used_transport_means$identifier = :identifier
    union all
    select from transport_equipment equipment
    where equipment.consignment_id = consignment.id and
          equipment.identifier = :identifier
    union all
    select from carried_transport_equipment equipment
    where equipment.consignment_id = consignment.id and
          equipment.identifier = :identifier))
limit :limit offset :offset;
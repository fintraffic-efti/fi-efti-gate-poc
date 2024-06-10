
create or replace view consignment_json as
select
  (select json_agg(movement) from transport_movement movement
   where movement.consignment_id = consignment.id) as main_carriage_transport_movements,
  (select json_agg(equipment) from (
    select
      (select json_agg(carried) from carried_transport_equipment carried
       where carried.consignment_id = consignment.id and
             carried.transport_equipment_ordinal = equipment.ordinal)
        as carried_transport_equipments,
      equipment.* from transport_equipment equipment
    where equipment.consignment_id = consignment.id) equipment) as utilized_transport_equipments,
  consignment.*
from consignment
;
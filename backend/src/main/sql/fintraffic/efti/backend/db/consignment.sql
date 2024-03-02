
-- :name select-consignment
select consignment.*
from consignment_json consignment
where consignment.gate_url = :gate-url and
      consignment.platform_url = :platform-url and
      consignment.data_id = :data-id
;

-- :name select-consignments
select consignment.* from consignment_json consignment
limit :limit offset :offset;
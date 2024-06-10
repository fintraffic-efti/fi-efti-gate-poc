call create_classification('transport_equipment_category'::name, 'text'::name);

create table transport_equipment (
  consignment_id     int not null references consignment (id),
  ordinal            int not null,

  category_code text not null references transport_equipment_category (id),
  dangerous_goods_indicator boolean,
  identifier text,
  sequence_numeric int,
  registration_country$id text references country (id),

  primary key (consignment_id, ordinal)
);

create table carried_transport_equipment (
  consignment_id     int not null references consignment (id),
  transport_equipment_ordinal int not null,
  ordinal            int not null,

  identifier text,
  sequence_numeric int,

  primary key (consignment_id, transport_equipment_ordinal, ordinal),
  foreign key (consignment_id, transport_equipment_ordinal)
    references transport_equipment (consignment_id, ordinal)
);
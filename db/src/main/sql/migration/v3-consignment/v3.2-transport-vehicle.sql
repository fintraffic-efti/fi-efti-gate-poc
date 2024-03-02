call create_classification('transport_mode'::name);

create table transport_vehicle (
  consignment_id     int not null references consignment (id),
  ordinal            int not null,

  transport_mode_id  smallint not null references transport_mode (id),
  vehicle_id         text,
  vehicle_country_id text references country (id),

  journey_start_time timestamptz,
  journey_end_time   timestamptz,
  country_start_id   text references country (id),
  country_end_id     text references country (id),

  primary key (consignment_id, ordinal)
);
call create_classification('transport_mode'::name);

create table transport_movement (
  consignment_id int not null references consignment (id),
  ordinal        int not null,

  transport_mode_code smallint not null references transport_mode (id),
  dangerous_goods_indicator boolean,
  used_transport_means$identifier text,
  used_transport_means$registration_country$id text references country (id),

  primary key (consignment_id, ordinal)
);
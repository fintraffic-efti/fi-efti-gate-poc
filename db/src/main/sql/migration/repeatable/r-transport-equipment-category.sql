insert into transport_equipment_category (id, label_en)
values
('T1', 'Tank-vehicle')
on conflict (id) do update set
  label_en = excluded.label_en,
  label_fi = excluded.label_fi,
  label_sv = excluded.label_sv;

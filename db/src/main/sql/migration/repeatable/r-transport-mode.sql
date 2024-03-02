insert into transport_mode (id, label_en)
values
(1, 'Road'),
(2, 'Railway'),
(3, 'Air'),
(4, 'Waterway')
on conflict (id) do update set
  label_en = excluded.label_en,
  label_fi = excluded.label_fi,
  label_sv = excluded.label_sv;

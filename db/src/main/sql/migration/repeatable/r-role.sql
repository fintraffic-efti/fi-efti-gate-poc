insert into role (id, label_en, label_fi, label_sv)
values
(-1, 'System', 'Järjestelmä', 'System'),
(0, 'Platform', 'Alusta', 'Platform')
on conflict (id) do update set
  label_en = excluded.label_en,
  label_fi = excluded.label_fi,
  label_sv = excluded.label_sv
;

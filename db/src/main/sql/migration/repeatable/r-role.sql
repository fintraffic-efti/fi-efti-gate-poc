insert into role (id, label_en, label_fi, label_sv, description)
values
(-1, 'System', 'Järjestelmä', 'System', null),
(0, 'Platform', 'Alusta', 'Platform', null),
(1, 'CA system', 'Viranomaisen järjestelmä', 'CA system', 'Competent authority system')
on conflict (id) do update set
  label_en = excluded.label_en,
  label_fi = excluded.label_fi,
  label_sv = excluded.label_sv
;

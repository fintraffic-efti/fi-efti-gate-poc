insert into ed_message_direction (id, label_en)
values
(1, 'In'),
(2, 'Out')
on conflict (id) do update set
   label_en = excluded.label_en;

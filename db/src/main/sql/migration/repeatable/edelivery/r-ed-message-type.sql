
insert into ed_message_type (id, label_en)
values
 (1, 'Test message')
on conflict (id) do update set
   label_en = excluded.label_en;

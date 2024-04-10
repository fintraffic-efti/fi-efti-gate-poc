
insert into ed_message_type (id, label_en)
values
 (0, 'Response'),
 (1, 'Find consignment'),
 (2, 'Find consignments')
on conflict (id) do update set
   label_en = excluded.label_en;

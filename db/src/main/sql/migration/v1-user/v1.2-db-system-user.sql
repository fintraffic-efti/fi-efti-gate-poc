
insert into role (id, label_en, label_fi, label_sv) values
(-1, 'System', 'Järjestelmä', 'System');

insert into end_user (id, role_id, first_name, last_name, email) values
(0, -1, 'database', 'efti', 'database@efti.finntraffic.fi')

-- :name select-whoami
select id, role_id, ssn, first_name, last_name, email, cognito_id from end_user
where role_id > -1 and (ssn = :ssn or email = :email);

-- :name select-users
select id, role_id, name, first_name, last_name from end_user;

-- :name upsert-end-user! :<!
insert into end_user (
  role_id, ssn, email, cognito_id,
  first_name, last_name)
values (
  :role-id, :ssn, :email, :cognito-id,
  :first-name, :last-name
)
on conflict (:i:unique-field) do nothing
returning id;

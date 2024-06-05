create user efti with createdb createrole password 'efti';
create user efti_gateway with password 'efti';

create database efti_template with is_template true;
create database efti_dev;
create database efti_fi2;

grant all privileges on database efti_template to efti;
grant all privileges on database efti_dev to efti;
grant all privileges on database efti_fi2 to efti;

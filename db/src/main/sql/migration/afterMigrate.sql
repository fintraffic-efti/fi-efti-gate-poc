-- application db users create & grants --
call create_db_user('efti_gateway'::name, '${gateway-password}');

grant select on all tables in schema efti, audit to efti_gateway;
grant execute on all functions in schema efti to efti_gateway;
grant usage on schema efti to efti_gateway;
grant usage on schema audit to efti_gateway;

-- insert/update privileges
grant insert, update on table end_user to efti_gateway;
grant insert, update on table consignment to efti_gateway;
grant insert, update on table transport_vehicle to efti_gateway;

-- insert privileges
grant insert on table ed_message to efti_gateway;

-- see https://en.wikipedia.org/wiki/Country_code

create table country (
  id char(2) primary key,
  alpha3 char(3),
  numeric int,
  label_en text,
  label_fi text,
  label_sv text,
  ordinal int not null default 0,
  valid boolean not null default true
);
call audit.activate('country'::name);

create table language (
  id char(3) primary key, -- ISO 639-2/T code
  alpha3b char(3),        -- ISO 639-2/B code
  alpha2 char(2),         -- ISO 639-1 code
  label_native text,
  label_en text,
  label_fi text,
  label_sv text,
  ordinal int not null default 0,
  valid boolean not null default true
);
call audit.activate('language'::name);

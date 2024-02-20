create temporary table language_tmp (
  iso_639_1 text,
  iso_639_2_T text,
  iso_639_2_B text,
  name text,
  native text
);

COPY language_tmp FROM STDIN with (format csv, delimiter ',', header true);
${file:language.csv}
\.

insert into language (id, alpha3b, alpha2, label_native, label_en)
select
  trim(iso_639_2_T), trim(iso_639_2_B), trim(iso_639_1), trim(native), trim(name)
from language_tmp on conflict (id) do update set
 alpha3b = excluded.alpha3b,
 alpha2 = excluded.alpha2,
 label_native = excluded.label_native,
 label_en = excluded.label_en;

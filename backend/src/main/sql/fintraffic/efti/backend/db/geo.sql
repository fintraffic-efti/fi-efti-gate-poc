
-- :name select-countries
select id, label_en, label_fi, label_sv, alpha3, valid from country
where label_fi is not null and label_sv is not null

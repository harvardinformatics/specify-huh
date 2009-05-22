select ts.id,
       ts.specimen_id,
       ts.taxon_id,
       (select name from st_lookup where id=ts.type_status_id) as type_status,
       (select name from st_lookup where id=ts.conditionality_id) as conditionality,
       decode(ts.fragment_flag, 1, 'true', '') as is_fragment,
       ts.verified_year,
       ts.verified_month,
       ts.verified_day,
       ts.nle1_designator_id,
       ts.nle1_publication_id,
       ts.nle1_collation,
       ts.nle1_date,
       ts.nle2_designator_id,
       ts.nle2_publication_id,
       ts.nle2_collation,
       ts.nle2_date,
       ts.remarks

from type_specimen ts

where (select name from st_lookup where id=ts.type_status_id) != 'Not a type'
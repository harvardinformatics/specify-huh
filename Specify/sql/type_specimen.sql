select ts.id,
       ts.specimen_id,
       (select acronym from organization where id=s.id) as collection_code,
       ts.taxon_id,
       (select name from st_lookup where id=ts.type_status_id) as type_status,
       (select name from st_lookup where id=ts.conditionality_id) as conditionality,
       decode(ts.fragment_flag, 1, 'true', '') as is_fragment,
       ts.verified_year,
       ts.verified_month,
       ts.verified_day,
       nvl((select name from botanist_name where type_id=110101 and botanist_id=ts.nle1_designator_id),
           (select name from botanist_name where type_id=110103 and botanist_id=ts.nle1_designator_id)) as nle1_designator,
       ts.nle1_publication_id,
       ts.nle1_collation,
       ts.nle1_date,
       nvl((select name from botanist_name where type_id=110101 and botanist_id=ts.nle2_designator_id),
           (select name from botanist_name where type_id=110103 and botanist_id=ts.nle2_designator_id)) as nle2_designator,
       ts.nle2_publication_id,
       ts.nle2_collation,
       ts.nle2_date,
       ts.remarks

from type_specimen ts, specimen s

where ts.specimen_id=s.id and (select name from st_lookup where id=ts.type_status_id) != 'Not a type'
select ts.id,
       ts.specimen_id,
       (select acronym from organization where id=s.herbarium_id) as collection_code,
       ts.taxon_id,
       (select decode(name, '[Neosyntype]', 'Neosyntype', name) from st_lookup where id=ts.type_status_id) as type_status,
       (select name from st_lookup where id=ts.conditionality_id) as conditionality,
       decode(ts.fragment_flag, 1, 'true', '') as is_fragment,
       decode(ts.verified_year, 0, null, ts.verified_year) as verified_year,
       decode(ts.verified_month, 0, null, ts.verified_month) as verified_month,
       decode(ts.verified_day, 0, null, ts.verified_day) as verified_day,
       regexp_replace(ts.verified_by, '[[:space:]]+', ' ') as verified_by,
       regexp_replace(nvl((select name from botanist_name where type_id=110101 and botanist_id=ts.nle1_designator_id),
                          (select name from botanist_name where type_id=110103 and botanist_id=ts.nle1_designator_id)),
                      '[[:space:]]+', ' ') as nle1_designator,
       ts.nle1_publication_id,
       regexp_replace(ts.nle1_collation, '[[:space:]]+', ' ') as nle1_collation,
       regexp_replace(ts.nle1_date, '[[:space:]]+', ' ') as nle1_date,
       regexp_replace(nvl((select name from botanist_name where type_id=110101 and botanist_id=ts.nle2_designator_id),
                          (select name from botanist_name where type_id=110103 and botanist_id=ts.nle2_designator_id)),
                       '[[:space:]]+', ' ') as nle2_designator,
       ts.nle2_publication_id,
       regexp_replace(ts.nle2_collation, '[[:space:]]+', ' ') as nle2_collation,
       regexp_replace(ts.nle2_date, '[[:space:]]+', ' ') as nle2_date,
       regexp_replace(ts.remarks, '[[:space:]]+', ' ') as remarks,
       ts.ordinal as ordinal

from type_specimen ts, specimen s

where ts.specimen_id=s.id and (select name from st_lookup where id=ts.type_status_id) != 'Not a type'
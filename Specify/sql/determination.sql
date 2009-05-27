select d.id,
       d.specimen_id,
       (select acronym from organization where id=s.herbarium_id) as collection_code,
       d.taxon_id,
       (select name from st_lookup where id=d.qualifier_id) as qualifier,
       d.det_year,
       d.det_month,
       d.det_day,
       decode(d.current_flag, 1, 'true', '') as is_current,
       decode(d.label_flag, 1, 'true', '') as is_label,
       d.determined_by,
       d.label_text,
       d.ordinal,
       d.remarks
from
       determination d, specimen s
where
       d.specimen_id=s.id

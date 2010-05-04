select d.id,
       d.specimen_id,
       (select acronym from organization where id=s.herbarium_id) as collection_code,
       d.taxon_id,
       (select name from st_lookup where id=d.qualifier_id) as qualifier,
       decode(d.det_year, 0, null, d.det_year) as det_year,
       decode(d.det_month, 0, null, d.det_month) as det_month,
       decode(d.det_day, 0, null, d.det_day) as det_day,
       decode(d.current_flag, 1, 'true', '') as is_current,
       decode(d.label_flag, 1, 'true', '') as is_label,
       regexp_replace(d.determined_by, '[[:space:]]+', ' ') as determined_by,
       regexp_replace(d.label_text, '[[:space:]]+', ' ') as label_text,
       d.ordinal,
       regexp_replace(d.remarks, '[[:space:]]+', ' ') as remarks

from determination d left join specimen s on d.specimen_id=s.id
order by d.id
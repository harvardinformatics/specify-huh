select s.id,
       (select acronym from organization where id=s.herbarium_id) as collection_code,
       s.taxon_group_id,
       regexp_replace(s.name, '[[:space:]]+', ' ') as name,
       regexp_replace(s.author, '[[:space:]]+', ' ') as author,
       bn.botanist_id,
       regexp_replace(s.specimen_count, '[[:space:]]+', ' ') as specimen_count,
       regexp_replace(s.location, '[[:space:]]+', ' ') as location,
       regexp_replace(s.cabinet, '[[:space:]]+', ' ') as cabinet,
       s.created_by_id,
       to_char(s.create_date, 'YYYY-MM-DD HH24:MI:SS') as date_created,
       regexp_replace(s.remarks, '[[:space:]]+', ' ') as remarks
from
       subcollection s,
       (select distinct botanist_id, name from botanist_name where type_id in (110102,110105)) bn

where s.author=bn.name(+)

order by s.id
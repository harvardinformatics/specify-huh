select bn.botanist_id,
       (select name from st_lookup where id=bn.type_id) as name_type,
       regexp_replace(bn.name, '[[:space:]]+', ' ') as name

from botanist_name bn

order by bn.botanist_id
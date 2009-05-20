select b.id,
       decode(b.team_flag, 1, 'true', '') as is_team,
       decode(b.corporate_flag, 1, 'true', '') as is_corporate,
       regexp_replace(nvl((select bn.name from botanist_name bn where bn.type_id=110101 and bn.botanist_id=b.id),
                          (select bn.name from botanist_name bn where bn.type_id=110103 and bn.botanist_id=b.id)), '[[:space:]]+', ' ') as name,
       (select name from st_lookup where id=b.dates_type_id) as dates_type,
       b.start_year,
       decode(b.start_precision_id, 113402, '?', 113403, 'circa', '') as start_precision,
       b.end_year,
       decode(b.end_precision_id, 113402, '?', 113403, 'circa', '') as end_precision,
       regexp_replace(b.remarks, '[[:space:]]+', ' ') as remarks,
       b.created_by_id,
       to_char(b.create_date, 'YYYY-MM-DD HH24:MI:SS') as date_created
from botanist b

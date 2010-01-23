select g.id,
       g.created_by_id,
       to_char(g.create_date, 'YYYY-MM-DD HH24:MI:SS') as date_created,
       g.updated_by_id,
       to_char(g.update_date, 'YYYY-MM-DD HH24:MI:SS') as date_updated,
       g.container_id,
       (select name from st_lookup where id=g.rank_id) as rank,
       g.iso_code,
       g.display_qualifier,
       (select name from geo_name where geo_unit_id=g.id and type_id=110701) as name,
       (select name from geo_name where geo_unit_id=g.id and type_id=110704) as vernacular_name,
       regexp_replace(g.remarks, '[[:space:]]+', ' ') as remarks,
       (select max(name) from (select * from geo_name order by name) where type_id=110703 and geo_unit_id=g.id and rownum <=1) as variant1,
       (select decode(count(name), 1, '', max(name)) from (select * from geo_name order by name) where type_id=110703 and geo_unit_id=g.id and rownum <=2) as variant2,
       (select decode(count(name), 1, '', 2, '', max(name)) from (select * from geo_name order by name) where type_id=110703 and geo_unit_id=g.id and rownum <=3) as variant3,
       (select decode(count(name), 1, '', 2, '', 3, '', max(name)) from (select * from geo_name order by name) where type_id=110703 and geo_unit_id=g.id and rownum <=4) as variant4,
       (select decode(count(name), 1, '', 2, '', 3, '', 4, '', max(name)) from (select * from geo_name order by name) where type_id=110703 and geo_unit_id=g.id and rownum <=5) as variant5
from
       geo_unit g

start with g.rank_id = 110601 /* this gets around the cycle created by the root node being its own container */

connect by prior g.id=g.container_id
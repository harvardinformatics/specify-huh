select o.id,
       regexp_replace(o.name, '[[:space:]]+', ' ') as name,
       o.acronym,
       (select name from geo_name where type_id=110701 and geo_unit_id=o.city_id) as city,
       (select name from geo_name where type_id=110701 and geo_unit_id=cities_in_states.state_id) as state,
       decode(cities_in_states.city_id, null,
              (select name from geo_name where type_id=110701 and geo_unit_id=cities_in_countries.country_id),
              (select name from geo_name where type_id=110701 and geo_unit_id=cities_in_states.country_id)) as country,
       regexp_replace(o.uri, '[[:space:]]+', ' ') as uri,
       o.created_by_id,
       to_char(o.create_date, 'YYYY-MM-DD HH24:MI:SS') as date_created,
       regexp_replace(o.remarks, '[[:space:]]', ' ') as remarks
from
       organization o,
       (select city_id,
               states.id as state_id,
               states.country_id
          from
               (select id as city_id, container_id from geo_unit where rank_id=110606) cities,
               (select id, rank_id, connect_by_root id as country_id
                from geo_unit
                where rank_id=110604
                start with rank_id=110603
                connect by prior id=container_id
                ) states
         where states.id=cities.container_id or states.id=(select container_id from geo_unit where id=cities.container_id)
      ) cities_in_states,
      (select a.id, a.container_id as country_id from geo_unit a where a.rank_id=110606 and (select b.rank_id from geo_unit b where a.container_id=b.id)=110603) cities_in_countries

where o.city_id=cities_in_states.city_id(+) and o.city_id=cities_in_countries.id(+)
order by o.id
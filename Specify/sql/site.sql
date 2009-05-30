select s.id,
       s.geo_unit_id,
       regexp_replace(s.locality, '[[:space:]]+', ' ') as locality,
       (select name from st_lookup where id=s.method_id) as latlong_method,
       s.latitude_a,
       s.longitude_a,
       s.latitude_b,
       s.longitude_b,
       s.elev_from,
       s.elev_to,
       (select name from st_lookup where id=s.elev_method_id) as elev_method

from site s

where not(s.locality is null and
          s.latitude_a is null and
          s.latitude_b is null and
          s.longitude_a is null and
          s.longitude_b is null and
          s.elev_from is null and
          s.elev_to is null)

order by s.geo_unit_id

select b.id as botanist_id,
       (select name from st_lookup where id=br.type_id) as role,
       g.id as geo_unit_id,
       brc.ordinal as order_number
from
       botanist b
       join botanist_role br on b.id=br.botanist_id
       join botanist_role_country brc on br.id=brc.botanist_role_id
       join geo_unit g on brc.country_id=g.id

order by
       b.id, role, order_number, brc.id
/* 
    The sequencing by ordinal field is known to be bad in our data.
    Furthermore, there are sequences that do not increase with increasing
    id.  However, if this query returns no results, you can sequence the
    improperly sequenced sets by increasing id.  (But not for the
    correctly sequenced sets, some of those have "out-of-order" ids but
    the ordinals make sense as a group.)
    
select a.id, b.id, a.botanist_role_id, a.ordinal, b.ordinal
  from botanist_role_country a, botanist_role_country b
 where a.botanist_role_id=b.botanist_role_id and
       a.id < b.id and
       a.ordinal > b.ordinal and
       a.id in
            (select
                brc.botanist_role_id
             from
                botanist_role_country brc
             group by
                brc.botanist_role_id
             having
                not(sum(brc.ordinal) = (count(brc.id) * (count(brc.id) + 1) / 2)) and
                not(sum(brc.ordinal+1) = (count(brc.id) * (count(brc.id) + 1) / 2))
            ) 
*/
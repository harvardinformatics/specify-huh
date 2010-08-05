select b.id as botanist_id,
       (select name from st_lookup where id=br.type_id) as role,
       s.name as specialty,
       brs.ordinal as order_number
from
       botanist b
       join botanist_role br on b.id=br.botanist_id
       join botanist_role_specialty brs on br.id=brs.botanist_role_id
       join specialty s on brs.specialty_id=s.id

order by
       b.id, role, brs.id

/*
  The sequencing by ordinal field is known to be bad in our data.
  But if this query is empty, you can ignore the ordinal and number
  the entries in order of increasing botanist_role_specialty.id:

select a.id, a.botanist_role_id, a.ordinal
from botanist_role_specialty a, botanist_role_specialty b
where 
   a.botanist_role_id=b.botanist_role_id and
   a.id < b.id and
   a.ordinal > b.ordinal
*/

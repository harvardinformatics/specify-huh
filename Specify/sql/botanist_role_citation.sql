select b.id as botanist_id,
       (select name from st_lookup where id=br.type_id) as role,
       brc.publication_id,
       brc.collation,
       brc.publ_date,
       brc.alt_source,
       brc.ordinal as order_number
from
       botanist b,
       botanist_role br,
       botanist_role_citation brc
where
       b.id=br.botanist_id and
       br.id=brc.botanist_role_id
order by
       b.id, role, brc.ordinal, brc.id
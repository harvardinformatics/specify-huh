select pn.publication_id,
       (select name from st_lookup where id=pn.type_id) as type,
       pn.text
from publ_number pn
order by pn.publication_id, (select name from st_lookup where id=pn.type_id), pn.text
select p.id, p.preceded_by_id, p.succeeded_by_id
from publication p
where p.preceded_by_id is not null or p.succeeded_by_id is not null
order by p.id, p.preceded_by_id
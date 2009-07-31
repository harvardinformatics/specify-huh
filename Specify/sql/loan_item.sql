select li.id,
       li.loan_id,
       to_char(li.return_date, 'YYYY-MM-DD HH24:MI:SS') as return_date,
       regexp_replace(li.barcode, '[[:space:]]+', ' ') as barcode,
       regexp_replace(li.transferred_from, '[[:space:]]+', ' ') as transferred_from,
       regexp_replace(li.transferred_to, '[[:space:]]+', ' ') as transferred_to,
       (select acronym from organization where id=s.herbarium_id) as collection,
       (select acronym from organization where id=ht.local_unit_id) as local_unit

from loan_item li,
     herb_transaction ht,
     (select specimen_id, barcode from specimen_item) si,
     (select id, herbarium_id from specimen) s

where l.barcode=si.barcode(+) and
      si.specimen_id=s.id(+) and
      li.loan_id=ht.id

/* select li.loan_id, li.barcode
from loan_item li, specimen_item si
where li.barcode=si.barcode(+)
group by li.barcode, li.loan_id
having count(si.barcode) < 1
order by li.loan_id, li.barcode */

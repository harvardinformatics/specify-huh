select li.id,
       li.loan_id,
       to_char(li.return_date, 'YYYY-MM-DD HH24:MI:SS') as return_date,
       regexp_replace(li.barcode, '[[:space:]]+', ' ') as barcode,
       regexp_replace(li.transferred_from, '[[:space:]]+', ' ') as transferred_from,
       regexp_replace(li.transferred_to, '[[:space:]]+', ' ') as transferred_to,
       (select acronym from organization where id=s.herbarium_id) as collection,
       (select acronym from organization where id=ht.local_unit_id) as local_unit,
       decode(count(ts.id), 0, 'false', 'true') as is_type

from loan_item li join 
     herb_transaction ht on li.loan_id=ht.id left join
     specimen_item si on li.barcode=si.barcode left join
     specimen s on si.specimen_id=s.id left join
     type_specimen ts on s.id=ts.specimen_id

group by li.id,
         li.loan_id,
         li.return_date,
         li.barcode,
         li.transferred_from,
         li.transferred_to,
         s.herbarium_id,
         ht.local_unit_id

order by li.id

/* select li.loan_id, li.barcode
from loan_item li, specimen_item si
where li.barcode=si.barcode(+)
group by li.barcode, li.loan_id
having count(si.barcode) < 1
order by li.loan_id, li.barcode */

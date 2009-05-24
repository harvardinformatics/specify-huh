select l.id,
       l.loan_id,
       to_char(l.return_date, 'YYYY-MM-DD HH24:MI:SS') as return_date,
       l.barcode,
       l.transferred_from,
       l.transferred_to,
       (select acronym from organization where id=s.herbarium_id) as collection

from loan_item l,
     (select specimen_id, barcode from specimen_item) si,
     (select id, herbarium_id from specimen) s

where l.barcode=si.barcode(+) and
      si.specimen_id=s.id(+)


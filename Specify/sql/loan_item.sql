select l.id,
       l.loan_id,
       to_char(l.return_date, 'YYYY-MM-DD HH24:MI:SS') as return_date,
       regexp_replace(l.barcode, '[[:space:]]+', ' ') as barcode,
       regexp_replace(l.transferred_from, '[[:space:]]+', ' ') as transferred_from,
       regexp_replace(l.transferred_to, '[[:space:]]+', ' ') as transferred_to,
       (select acronym from organization where id=s.herbarium_id) as collection

from loan_item l,
     (select specimen_id, barcode from specimen_item) si,
     (select id, herbarium_id from specimen) s

where l.barcode=si.barcode(+) and
      si.specimen_id=s.id(+)


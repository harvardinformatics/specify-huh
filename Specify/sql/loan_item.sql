select l.id,
       l.loan_id,
       to_char(l.return_date, 'YYYY-MM-DD HH24:MI:SS') as return_date,
       l.barcode,
       l.transferred_from,
       l.transferred_to,
       (select acronym from organization where id=items.herbarium_id) as collection

from loan_item l,
     (select s.specimen_id, s.herbarium_id, si.barcode
      from specimen_item si, specimen s
      where si.specimen_id=s.specimen_id) items

where l.barcode=items.barcode
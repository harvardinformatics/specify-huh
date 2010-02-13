select t.id,
       t.created_by_id,
       to_char(t.create_date, 'YYYY-MM-DD HH24:MI:SS') as date_created,
       t.updated_by_id,
       to_char(t.update_date, 'YYYY-MM-DD HH24:MI:SS') as date_updated,
       (select name from st_lookup where id=t.type_id) as transaction_type,
       t.agent_id,
       a.organization_id,
       (select acronym from organization where id=t.local_unit_id) as local_unit,
       (select name from st_lookup where id=t.request_type_id) as request_type,
       (select name from st_lookup where id=t.purpose_id) as purpose,
       t.affiliate_id,
       (select name from st_lookup where id=t.user_type_id) as user_type,
       decode(t.acknowledged_flag, 1, 'true', '') as is_acknowledged,
       to_char(t.open_date, 'YYYY-MM-DD HH24:MI:SS') as date_opened,
       to_char(t.close_date, 'YYYY-MM-DD HH24:MI:SS') as date_closed,
       regexp_replace(t.transaction_no, '[[:space:]]+', ' ') as transaction_no,
       regexp_replace(t.for_use_by, '[[:space:]]+', ' ') as for_use_by,
       regexp_replace(t.box_count, '[[:space:]]+', ' ') as box_count,
       regexp_replace(t.description, '[[:space:]]+', ' ') as description,
       regexp_replace(t.remarks, '[[:space:]]+', ' ') as remarks,

       tb.item_count,
       tb.type_count,
       tb.non_specimen_count,

       to_char((select min(date_due) from due_date where loan_id=t.id), 'YYYY-MM-DD HH24:MI:SS') as original_due_date,
       to_char((select max(date_due) from due_date where loan_id=t.id), 'YYYY-MM-DD HH24:MI:SS') as current_due_date,

       (select name from taxon where id=tb.higher_taxon_id) as higher_taxon,
       regexp_replace(tb.taxon, '[[:space:]]+', ' ') as taxon,
       regexp_replace(tb.transferred_from, '[[:space:]]+', ' ') as transferred_from,
       return_items.quantity as quantity_returned

from herb_transaction t,
     agent a,
     taxon_batch tb,

     (select ht.id as borrow_id,
             sum(b.item_count + b.type_count + b.non_specimen_count) as quantity
      from herb_transaction ht,
           (select id, herb_transaction_id as borrow_id, item_count, type_count, non_specimen_count from out_return_batch) b
      where ht.id=b.borrow_id
      group by ht.id
     ) return_items

where (select name from st_lookup where id=t.type_id) = 'borrow' and
      t.id=tb.herb_transaction_id and
      t.agent_id=a.id(+) and
      t.id=return_items.borrow_id(+)

order by t.id
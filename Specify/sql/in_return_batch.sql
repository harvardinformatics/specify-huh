select irb.id,
       irb.herb_transaction_id,
       (select name from st_lookup where id=t.type_id) as type,
       irb.item_count,
       irb.non_specimen_count,
       irb.box_count,
       decode(irb.acknowledged_flag, 1, 'true', '') as is_acknowledged,
       to_char(irb.action_date, 'YYYY-MM-DD HH24:MI:SS') as action_date,
       regexp_replace(irb.transferred_to, '[[:space:]]+', ' ') as transferred_to

from in_return_batch irb,
     herb_transaction t

where irb.herb_transaction_id=t.id
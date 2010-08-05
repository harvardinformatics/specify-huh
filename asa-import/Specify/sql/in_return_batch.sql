select irb.id,
       irb.herb_transaction_id,
       (select acronym from organization where id=t.local_unit_id) as collection_code,
       irb.item_count,
       0 as type_count,
       irb.non_specimen_count,
       irb.box_count,
       decode(irb.acknowledged_flag, 1, 'true', '') as is_acknowledged,
       to_char(irb.action_date, 'YYYY-MM-DD HH24:MI:SS') as action_date,
       regexp_replace(irb.transferred_to, '[[:space:]]+', ' ') as transferred_to

from in_return_batch irb left join
     herb_transaction t on irb.herb_transaction_id=t.id

order by irb.id
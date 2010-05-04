select orb.id,
       orb.herb_transaction_id,
       (select acronym from organization where id=t.local_unit_id) as collection_code,
       orb.item_count,
       orb.type_count,
       orb.non_specimen_count,
       orb.box_count,
       decode(orb.acknowledged_flag, 1, 'true', '') as is_acknowledged,
       to_char(orb.action_date, 'YYYY-MM-DD HH24:MI:SS') as action_date,
       (select name from st_lookup where id=orb.carrier_id) as carrier,
       (select name from st_lookup where id=orb.method_id) as method,
       orb.cost,
       decode(orb.cost_estimated_flag, 1,'true', '') as is_estimated_cost,
       regexp_replace(orb.note, '[[:space:]]+', ' ') as note

from out_return_batch orb left join
     herb_transaction t on orb.herb_transaction_id=t.id

order by orb.id
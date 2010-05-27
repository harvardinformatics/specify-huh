select s.id,
       s.herb_transaction_id,
       (select name from st_lookup where id=t.type_id) as transaction_type,
       (select name from st_lookup where id=s.carrier_id) as carrier,
       (select name from st_lookup where id=s.method_id) as method,
       s.cost,
       decode(s.cost_estimated_flag, 1,'true', '') as is_estimated_cost,
       decode(s.insurance_flag, 1, 'true', '') as is_insured,
       s.ordinal,
       regexp_replace(s.tracking_no, '[[:space:]]+', ' ') as tracking_no,
       regexp_replace(s.customs_no, '[[:space:]]+', ' ') as customs_no,
       regexp_replace(s.description, '[[:space:]]+', ' ') as description,
       regexp_replace(t.box_count, '[[:space:]]+', ' ') as box_count,
       (select acronym from organization where id=t.local_unit_id) as collection_code,
       a.id,
       a.organization_id,
       to_char(t.open_date, 'YYYY-MM-DD HH24:MI:SS') as shipment_date

from shipment s left join
     herb_transaction t on s.herb_transaction_id=t.id left join
     agent a on t.agent_id=a.id

where (select name from st_lookup where id=t.type_id) != 'outgoing miscellaneous'

order by s.id
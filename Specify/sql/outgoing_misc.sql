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

       decode(s.carrier_id, null, 'NA/Unknown', (select name from st_lookup where id=s.carrier_id)) as carrier,
       decode(s.method_id, null, 'NA/Unknown', (select name from st_lookup where id=s.method_id)) as method,
       s.cost,
       decode(s.cost_estimated_flag, 1,'true', '') as is_estimated_cost,
       decode(s.insurance_flag, 1, 'true', '') as is_insured,
       s.ordinal,
       regexp_replace(s.tracking_no, '[[:space:]]+', ' ') as tracking_no,
       regexp_replace(s.customs_no, '[[:space:]]+', ' ') as customs_no,
       regexp_replace(s.description, '[[:space:]]+', ' ') as description,
       regexp_replace(t.box_count, '[[:space:]]+', ' ') as box_count

from herb_transaction t,
     agent a,
     shipment s

where t.id=s.herb_transaction_id(+) and
      (select name from st_lookup where id=t.type_id) = 'outgoing miscellaneous' and
      t.agent_id=a.id(+)

order by t.id
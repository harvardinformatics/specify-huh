select s.id,
       s.herb_transaction_id,
       (select name from st_lookup where id=s.carrier_id) as carrier,
       (select name from st_lookup where id=s.method_id) as method,
       s.cost,
       decode(s.cost_estimated_flag, 1,'true', '') as is_estimated_cost,
       decode(s.insurance_flag, 1, 'true', '') as is_insured,
       s.ordinal,
       s.tracking_no,
       s.customs_no,
       regexp_replace(s.description, '[[:space:]]+', ' ') as description

from shipment s
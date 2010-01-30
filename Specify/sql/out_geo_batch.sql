select ogb.id,
       ogb.herb_transaction_id,
       (select name from st_lookup where id=t.type_id) as transaction_type,
       (select name from st_lookup where id=t.request_type_id) as request_type,
       (select acronym from organization where id=t.local_unit_id) as collection_code,
       ogb.item_count,
       ogb.type_count,
       ogb.non_specimen_count,
       (select name from geo_name where geo_unit_id=ogb.geo_region_id and type_id=110701) as geo_unit
       
from out_geo_batch ogb,
     herb_transaction t

where ogb.herb_transaction_id=t.id and
      (select name from st_lookup where id=t.type_id) in ('outgoing gift','outgoing exchange','outgoing special exch')

order by t.id
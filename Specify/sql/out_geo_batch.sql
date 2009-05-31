select ogb.id,
       ogb.herb_transaction_id,
       (select name from st_lookup where id=t.type_id) as type,
       (select name from geo_name where geo_unit_id=ogb.geo_region_id and type_id=110701) as geo_unit,
       ogb.item_count,
       ogb.type_count,
       ogb.non_specimen_count

from out_geo_batch ogb,
     herb_transaction t

where ogb.herb_transaction_id=t.id and
      (select name from st_lookup where id=t.type_id) not in ('outgoing exchange', 'outgoing special exch')
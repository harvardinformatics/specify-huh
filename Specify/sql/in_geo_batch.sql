select igb.id,
       igb.herb_transaction_id,
       (select name from st_lookup where id=t.type_id) as type,
       (select name from geo_name where geo_unit_id=igb.geo_region_id and type_id=110701) as src_geography,
       igb.item_count,
       igb.type_count,
       igb.non_specimen_count,
       igb.discard_count,
       igb.return_count,
       igb.cost

from in_geo_batch igb,
     herb_transaction t

where igb.herb_transaction_id=t.id
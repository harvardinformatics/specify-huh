select igb.id,
       igb.herb_transaction_id,
       igb.item_count,
       igb.type_count,
       igb.non_specimen_count,
       igb.geo_region_id,
       igb.discard_count,
       igb.distribute_count,
       igb.return_count,
       igb.cost

from in_geo_batch igb

order by igb.id
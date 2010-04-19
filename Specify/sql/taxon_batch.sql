select tb.id,
       tb.herb_transaction_id,
       (select acronym from organization where id=t.local_unit_id) as collection_code,
       (select name from st_lookup where id=t.type_id) as type,
       tb.higher_taxon_id,
       tb.item_count,
       tb.type_count,
       tb.non_specimen_count,
       regexp_replace(tb.taxon, '[[:space:]]+', ' ') as taxon,
       regexp_replace(tb.transferred_from, '[[:space:]]+', ' ') as transferred_from,
       nvl(decode((select name from st_lookup where id=t.type_id),
              'borrow', (select sum(item_count + type_count + non_specimen_count) from out_return_batch where herb_transaction_id=t.id),
              'loan',   (select sum(item_count + type_count + non_specimen_count) from in_return_batch where herb_transaction_id=t.id)
          ), 0
       ) as qty_returned

from taxon_batch tb,
     herb_transaction t

where tb.herb_transaction_id=t.id

/* check to make sure there isn't both an in_geo_batch and an out_geo_batch for a transaction */
/*
select t.id
from herb_transaction t
where exists (select null from in_geo_batch igb where igb.herb_transaction_id=t.id) and
      exists (select null from out_geo_batch ogb where ogb.herb_transaction_id=t.id)
*/
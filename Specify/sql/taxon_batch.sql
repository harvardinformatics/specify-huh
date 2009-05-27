select tb.id,
       tb.herb_transaction_id,
       (select name from st_lookup where id=t.type_id) as type,
       (select name from taxon where id=tb.higher_taxon_id) as higher_taxon,
       tb.item_count,
       tb.type_count,
       tb.non_specimen_count,
       regexp_replace(tb.taxon, '[[:space:]]+', ' ') as taxon,
       regexp_replace(tb.transferred_from, '[[:space:]]+', ' ') as transferred_from

from taxon_batch tb,
     herb_transaction t

where tb.herb_transaction_id=t.id
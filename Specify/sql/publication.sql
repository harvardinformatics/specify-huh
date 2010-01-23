select p.id,
       p.created_by_id,
       to_char(p.create_date, 'YYYY-MM-DD HH24:MI:SS') as date_created,
       p.updated_by_id,
       to_char(p.update_date, 'YYYY-MM-DD HH24:MI:SS') as date_updated,
       (select min(text) from publ_number where type_id=110404 and publication_id=p.id) as isbn,
       regexp_replace(p.publ_place, '[[:space:]]+', ' ') as pub_place,
       regexp_replace(p.publisher, '[[:space:]]+', ' ') as publisher,
       regexp_replace(p.uri, '[[:space:]]+', ' ') as url,
       regexp_replace((select title from publ_title pt where pt.publication_id=p.id and pt.type_id=110201), '[[:space:]]+', ' ') as title,
       regexp_replace(p.publ_date, '[[:space:]]+', ' ') as publ_date,
       decode(nvl((select max(pn.id)
              from publ_number pn
             where pn.publication_id=p.id and pn.type_id in (110401, 110402, 110405)),
            (select max(pt.id) 
               from publ_title pt
              where p.id=pt.publication_id and (pt.title like '%vista%' or pt.title like '%evue%' or pt.title like '%schrift%' or pt.title like '%ournal%')
            )), null, '', 'true') as is_journal,
       (select text from publ_number where type_id=110405 and publication_id=p.id) as issn,
       nvl((select text from publ_number where type_id=110401 and publication_id=p.id),
           (select decode(max(text), null, '', min(text), 'BPH2 ' || max(text), 'BPH2 ' || max(text) || '; ' || min(text)) from publ_number where type_id=110402 and publication_id=p.id)
          ) as bph,
       regexp_replace((select title from publ_title pt where pt.publication_id=p.id and pt.type_id=110203), '[[:space:]]+', ' ') as abbrev,
       regexp_replace(p.remarks, '[[:space:]]+', ' ') as remarks

from
      publication p

order by
      p.id
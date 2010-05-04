select p.id,
       p.created_by_id,
       to_char(p.create_date, 'YYYY-MM-DD HH24:MI:SS') as date_created,
       p.updated_by_id,
       to_char(p.update_date, 'YYYY-MM-DD HH24:MI:SS') as date_updated,
       regexp_replace(p.publ_place, '[[:space:]]+', ' ') as pub_place,
       regexp_replace(p.publisher, '[[:space:]]+', ' ') as publisher,
       regexp_replace(p.uri, '[[:space:]]+', ' ') as url,
       regexp_replace((select title from publ_title pt where pt.publication_id=p.id and pt.type_id=110201), '[[:space:]]+', ' ') as title,
       regexp_replace(p.publ_date, '[[:space:]]+', ' ') as publ_date,
       regexp_replace((select title from publ_title pt where pt.publication_id=p.id and pt.type_id=110203), '[[:space:]]+', ' ') as abbrev,
       regexp_replace(p.remarks, '[[:space:]]+', ' ') as remarks

from
      publication p

order by
      p.id
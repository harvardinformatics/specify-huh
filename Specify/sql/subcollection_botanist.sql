select s.id as subcollection_id,
       b.id as botanist_id,
       (select fullname from taxon where id=s.taxon_group_id) as taxon,
       regexp_replace(s.name, '[[:space:]]+', ' ') as name,
       regexp_replace(s.author, '[[:space:]]+', ' ') as author,
       regexp_replace(bn.name, '[[:space:]]+', ' ') as botanist_name,
       (select decode(name, 'collected', 'c.', 'flourished', 'f.', 'b.') from st_lookup where id=b.dates_type_id) || ' ' || b.start_year || '-' || b.end_year as dates,
       (select note from botanist_role where type_id=110901 and botanist_id=b.id) as author_note
from 
       (select id,
               taxon_group_id,
               author,
               substr(author, 0, instr(author, '.')-1) as author_match,
               name
          from subcollection
         where length(author) > 4 and instr(author, '.') > 4
       ) s,
       (select id, start_year, end_year, dates_type_id from botanist where team_flag=0) b,
       (select distinct botanist_id, name from botanist_name where type_id in (110101)) bn

where
      length(bn.name) >= length(s.author_match) and
      substr(bn.name, 0, length(s.author_match)) = s.author_match and
      bn.botanist_id=b.id

order by s.id

/*
select s.id,
       (select fullname from taxon where id=s.taxon_group_id) as taxon,
       regexp_replace(s.name, '[[:space:]]+', ' ') as name,
       regexp_replace(s.author, '[[:space:]]+', ' ') as author
from
     subcollection s
where
     s.id not in (

     select sub.id
       from
           (select id, author from subcollection where author is not null) sub,
           (select id from botanist where team_flag=0) b,
           (select distinct botanist_id, name from botanist_name where type_id in (110101)) bn

      where length(s.author) > 4 and instr(s.author, '.') > 4 and length(bn.name) >= length(s.author) and
            substr(s.author, 0, instr(s.author, '.')-1) = substr(bn.name, 0, instr(s.author, '.')-1) and
            bn.botanist_id=b.id
     )
*/
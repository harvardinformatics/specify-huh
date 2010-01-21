select regexp_replace(decode(tx.rank_type, 'infraspecific', (select name from taxon where id=grandparent.id) || ' ' ||
                                             (select name from taxon where id=parent.id) || ' ' ||
                                             tx.rank_abbrev || tx.name,
                                'specific', (select name from taxon where id=parent.id) || ' ' || tx.name,
                                  tx.name
              ), '[[:space:]]+', ' ') as fullname,

              tx.*
from
  (select t.container_id,
         t.id,
         tr.name as rank,
         (select name from st_lookup where id=tr.type_id) as rank_type,
         tr.abbrev as rank_abbrev,
         (select name from st_lookup where id=t.group_id) as grp,
         (select name from st_lookup where id=t.status_id) as status,
         (select decode(name, '[none]', '', name) from st_lookup where id=t.endangered_id) as endangerment,
         (decode(t.hybrid_flag, 1, 'true', '')) as is_hybrid,
         regexp_replace(t.fullname, '[[:space:]]+', ' ') as fullname,
         regexp_replace(t.name, '[[:space:]]+', ' ') as name,
         regexp_replace(
           concat(
             concat(
               decode(t.par_author_id, null, '',
                       '(' ||
                           concat((select name from botanist_name where botanist_id=t.par_author_id and type_id=110105),
                                  (select decode(name, null, '', ' ex ' || name) from botanist_name where botanist_id=t.par_ex_author_id and type_id=110105)
                                  )
                        || ') '
                      ),
               concat((select name from botanist_name where botanist_id=t.std_author_id and type_id=110105),
                      (select decode(name, null, '', ' ex ' || name) from botanist_name where botanist_id=t.std_ex_author_id and type_id=110105)
                      )
               ),
             (select decode(name, null, '', ' cit. in ' || name) from botanist_name where botanist_id=t.cit_in_author_id and type_id=110105)
           ),
           '[[:space:]]+',
           ' '
         )
         as author,
         t.par_author_id,
         t.par_ex_author_id,
         t.std_author_id,
         t.std_ex_author_id,
         t.cit_in_author_id,
         t.cit_publ_id,
         regexp_replace(t.cit_collation, '[[:space:]]+', ' ') as cit_collation,
         t.cit_date,
         regexp_replace(t.remarks, '[[:space:]]+', ' ') as remarks,
         t.created_by_id,
         to_char(t.create_date, 'YYYY-MM-DD HH24:MI:SS') as date_created,
         (select b.fullname from taxon b where b.id=t.basionym_id) as basionym

  from taxon t, taxon_rank tr
  where t.rank_id=tr.id
  start with t.container_id is null
  connect by prior t.id=t.container_id
  ) tx,
  taxon parent,
  taxon grandparent

where tx.container_id=parent.id(+) and
      parent.container_id=grandparent.id(+)
select pa.publication_id, pa.author_id, pa.ordinal
from publ_author pa
order by pa.publication_id, pa.ordinal, pa.id

/*
 The sequencing by ordinal field is known to be bad in our data.
 
select pb.id, pb.publication_id, pb.author_id, pb.ordinal
from publ_author pb
where pb.publication_id in
 (select pa.publication_id
   from publ_author pa
   group by pa.publication_id
   having not(sum(pa.ordinal) = (count(pa.id) * (count(pa.id) + 1) / 2)) and
         not(sum(pa.ordinal+1) = (count(pa.id) * (count(pa.id) + 1) / 2))
 )
order by pb.publication_id, pb.ordinal, pb.id


 But if this query is empty, you can ignore the ordinal and number the entries in order
 of increasing publ_author.id:

 select pb.id, pb.publication_id, pb.author_id, pb.ordinal
   from publ_author pa, publ_author pb
  where pa.publication_id=pb.publication_id and
        pa.id < pb.id and
        pa.ordinal > pb.ordinal
*/
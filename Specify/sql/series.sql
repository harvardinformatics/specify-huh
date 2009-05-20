select s.id,
       s.name,
       s.abbreviation,
       s.institution_id, /* note that all are null as of 2009-05-11 */
       s.note
from
       series s
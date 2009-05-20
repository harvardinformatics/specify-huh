select a.id,
       regexp_replace(a.surname, '[[:space:]]+', ' ') as surname,
       regexp_replace(a.given_name, '[[:space:]]+', ' ') as given_name,
       regexp_replace(a.position, '[[:space:]]+', ' ') as position,
       regexp_replace(a.phone, '[[:space:]]+', ' ') as phone,
       regexp_replace(a.email, '[[:space:]]+', ' ') as email,
       regexp_replace(a.address, '[[:space:]]+', ' ') as address,
       a.created_by_id,
       to_char(a.create_date, 'YYYY-MM-DD HH24:MI:SS') as date_created,
       a.remarks
from
       affiliate a

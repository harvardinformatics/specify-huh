select a.id,
       a.organization_id,
       decode(a.active_flag, 1, 'true', '') as is_active,
       regexp_replace(a.prefix, '[[:space:]]+', ' ') as prefix,
       regexp_replace(a.name, '[[:space:]]+', ' ') as name,
       regexp_replace(a.title, '[[:space:]]+', ' ') as title,
       regexp_replace(a.specialty, '[[:space:]]+', ' ') as specialty,
       regexp_replace(a.corresp_address, '[[:space:]]+', ' ') as corresp_address,
       regexp_replace(a.shipping_address, '[[:space:]]+', ' ') as shipping_address,
       regexp_replace(a.email, '[[:space:]]+', ' ') as email,
       regexp_replace(a.phone, '[[:space:]]+', ' ') as phone,
       regexp_replace(a.fax, '[[:space:]]+', ' ') as fax,
       regexp_replace(a.uri, '[[:space:]]+', ' ') as uri,
       regexp_replace(a.remarks, '[[:space:]]+', ' ') as remarks
from
       agent a
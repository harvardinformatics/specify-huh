select a.id,
       a.organization_id,
       decode(a.active_flag, 1, 'true', '') as is_active,
       regexp_replace(a.name, '[[:space:]]+', ' ') as name,
       a.title,
       regexp_replace(a.specialty, '[[:space:]]+', ' ') as specialty,
       regexp_replace(a.corresp_address, '[[:space:]]+', ' ') as corresp_address,
       regexp_replace(a.shipping_address, '[[:space:]]+', ' ') as shipping_address,
       a.email,
       a.phone,
       a.fax,
       a.uri,
       a.remarks
from
       agent a
select t.id,
       (select name from st_lookup where id=t.type_id) as transaction_type,
       t.agent_id,
       (select acronym from organization where id=t.local_unit_id) as local_unit,
       (select name from st_lookup where id=t.request_type_id) as request_type,
       (select name from st_lookup where id=t.purpose_id) as purpose,
       t.affiliate_id,
       (select name from st_lookup where id=t.user_type_id) as user_type,
       decode(t.acknowledged_flag, 1, 'true', '') as is_acknowledged,
       to_char(t.open_date, 'YYYY-MM-DD HH24:MI:SS') as date_opened,
       to_char(t.close_date, 'YYYY-MM-DD HH24:MI:SS') as date_closed,
       regexp_replace(t.transaction_no, '[[:space:]]+', ' ') as transaction_no,
       regexp_replace(t.for_use_by, '[[:space:]]+', ' ') as for_use_by,
       regexp_replace(t.box_count, '[[:space:]]+', ' ') as box_count,
       regexp_replace(t.description, '[[:space:]]+', ' ') as description,
       regexp_replace(t.remarks, '[[:space:]]+', ' ') as remarks,
       t.created_by_id,
       to_char(t.create_date, 'YYYY-MM-DD HH24:MI:SS') as date_created,
       to_char((select min(date_due) from due_date where id=t.id), 'YYYY-MM-DD HH24:MI:SS') as original_due_date,
       to_char((select max(date_due) from due_date where id=t.id), 'YYYY-MM-DD HH24:MI:SS') as current_due_date


from herb_transaction t
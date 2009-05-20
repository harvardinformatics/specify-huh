select si.id,
       s.id,
       regexp_replace(si.barcode, '[[:space:]]+', ' ') as barcode,
       regexp_replace(s.collector_no, '[[:space:]]+', ' ') as collector_no,
       to_char(s.create_date, 'YYYY-MM-DD HH24:MI:SS') as date_created,
       decode(s.cultivated_flag, 1, 'true', '') as is_cultivated,
       regexp_replace(s.description, '[[:space:]]+', ' ') as description,
       regexp_replace(s.habitat, '[[:space:]]+', ' ') as habitat,
       regexp_replace(s.substrate, '[[:space:]]+', ' ') as substrate,
       (select name from st_lookup where id=si.repro_id) as repro_status,
       (select name from st_lookup where id=si.sex_id) as sex,
       regexp_replace(s.remarks, '[[:space:]]+', ' ') as remarks,
       regexp_replace(si.accession_no, '[[:space:]]+', ' ') as accession_no,
       regexp_replace(si.provenance, '[[:space:]]+', ' ') as provenance,
       (select name from st_lookup where id=si.status_id) as accession_status,
       d.start_year,
       d.start_month,
       d.start_day,
       (select name from st_lookup where id=d.start_precision_id) as start_precision,
       d.end_year,
       d.end_month,
       d.end_day,
       (select name from st_lookup where id=d.end_precision_id) as end_precision,
       regexp_replace(d.text, '[[:space:]]+', ' ') as date_text,
       regexp_replace(si.item_no, '[[:space:]]+', ' ') as item_no,
       decode((select name from st_lookup where id=si.oversize_id), 'oversize', 'true', '') as is_oversize,
       regexp_replace(si.voucher, '[[:space:]]+', ' ') as voucher,
       regexp_replace(si.reference, '[[:space:]]+', ' ') as reference,
       regexp_replace(si.note, '[[:space:]]+', ' ') as note,
       (select acronym from organization where id=s.herbarium_id) as herbarium_code,
       s.series_id,
       s.site_id,
       b.id as cataloged_by_id,
       s.created_by_id,
       (select name from st_lookup where id=si.format_id) as format,
       regexp_replace(s.series_no, '[[:space:]]+', ' ') as series_number,
       regexp_replace(si.container, '[[:space:]]+', ' ') as container
from
       specimen_item si,
       specimen s,
       botanist b,
       bdate d

where
       si.specimen_id=s.id and
       s.collector_id=b.id and
       s.date_id=d.id

order by si.specimen_id
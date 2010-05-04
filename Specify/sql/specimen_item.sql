select si.id,
       s.created_by_id,
       to_char(s.create_date, 'YYYY-MM-DD HH24:MI:SS') as date_created,
       s.updated_by_id,
       to_char(s.update_date, 'YYYY-MM-DD HH24:MI:SS') as date_updated,
       s.id,
       regexp_replace(si.barcode, '[[:space:]]+', ' ') as barcode,
       regexp_replace(s.collector_no, '[[:space:]]+', ' ') as collector_no,
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
       (select name from series where id=s.series_id) as series_name,
       s.site_id,
       b.id as collector_id,
       (select name from st_lookup where id=si.format_id) as format,
       (select abbreviation from series where id=s.series_id) as series_abbrev,
       regexp_replace(s.series_no, '[[:space:]]+', ' ') as series_number,
       regexp_replace(si.container, '[[:space:]]+', ' ') as container,
       si.subcollection_id,
       si.replicates,
       regexp_replace(si.location || decode(si.temp_location, '', null, '; temporarily held: ' || si.temp_location), '[[:space:]]+', ' ') as location,
       regexp_replace(s.vernacular_name, '[[:space:]]+', ' ') as vernacular_name,
       regexp_replace(s.distribution, '[[:space:]]+', ' ') as distribution
from
       specimen s
       left join specimen_item si on s.id=si.specimen_id
       left join botanist b on s.collector_id=b.id
       left join bdate d on s.date_id=d.id

order by s.id, si.id

/*
select si.id, si.barcode

from specimen s, specimen_item si

where si.specimen_id=s.id and 
si.barcode in (
  select barcode from
    (select barcode, count(id) from specimen_item group by barcode having count(id) > 1)
)
order by si.barcode
*/
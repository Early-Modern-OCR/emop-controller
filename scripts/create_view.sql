--
-- Create a view that averages the page results per batch
--
create view work_ocr_results as
   select pg_work_id as work_id, 
          page_results.batch_id as batch_id,
          ocr_engine_id,
          avg(juxta_change_index) as juxta_accuracy, 
          avg(alt_change_index) as retas_accuracy
      from pages, page_results, batch_job where pg_page_id=page_id and batch_id=batch_job.id 
      group by batch_id;
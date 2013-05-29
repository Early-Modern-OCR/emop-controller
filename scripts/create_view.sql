--
-- Create a view that averages the most recent batch run result for non-gale OCR
--
create view work_ocr_results as
   select pg_work_id as work_id, 
          batch_id,
          ocr_completed,
          ocr_engine_id,
          avg(juxta_change_index) as juxta_accuracy, 
          avg(alt_change_index) as retas_accuracy
      from pages, page_results,batch_job  where pg_page_id=page_id and batch_id=batch_job.id and batch_id = 
      (select batch_id from pages, page_results, batch_job  
        where pg_page_id=page_id and batch_job.id=batch_id and ocr_engine_id!=1 
        order by ocr_completed desc limit 1);
	  
--
-- Create a view that averages the gale results
--
create view work_gale_results as
	select pg_work_id as work_id, 
          batch_id,
	       avg(juxta_change_index) as juxta_accuracy, 
	       avg(alt_change_index) as retas_accuracy
   from pages, page_results  where pg_page_id=page_id and batch_id = 
	   (select batch_id from pages, page_results, batch_job  
	     where pg_page_id=page_id and batch_job.id=batch_id and ocr_engine_id=1 
	     order by ocr_completed desc limit 1);
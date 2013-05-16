
-- add some testing data that set up a batch job to do a
-- gt comparison with one gale ocr'd work
insert into batch_job (ocr_engine_id, name, notes) values(1,"Gale Compare","Fake test job to compare gale with GT");
insert into job_queue (page_id, batch_id) select pg_page_id, 1 from pages where pg_work_id=151311;

-- add a job for OCR
insert into batch_job (ocr_engine_id, name, notes) values(2,"Tesseract OCR","Test job to use tesseract for OCR");
insert into job_queue (page_id, batch_id) select pg_page_id, 2 from pages where pg_work_id=151311;

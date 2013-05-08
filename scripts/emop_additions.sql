CREATE TABLE IF NOT EXISTS emop_job_queue (
	id BIGINT NOT NULL AUTO_INCREMENT,
	page_id int(11) NOT NULL,
	batch_id varchar(20) not null,
	job_status ENUM('NOT_STARTED','PROCESSING','PENDING_POSTPROCESS','DONE', 'FAILED') not null default 'NOT_STARTED',
	job_type ENUM('OCR_TESSERACT','OCR_GAMERA','OCR_OCROPUS','GT_COMPARE') not null,
	parameters varchar(255),
	created DATETIME not null,
	last_update DATETIME,
	results varchar(255),
	PRIMARY KEY (id),
	FOREIGN KEY (page_id) REFERENCES pages (pg_page_id) ON DELETE CASCADE,
	index(page_id),
	index (batch_id),
	unique(batch_id)
)  ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE IF NOT EXISTS page_ocr_results (
	id BIGINT NOT NULL AUTO_INCREMENT,
	page_id int(11) NOT NULL,
	ocr_engine ENUM('TESSERACT','GAMERA','OCROPUS') not null,
	ocr_text_path varchar(200) not null,
   ocr_xml_path varchar(200) not null,
	ocr_completed DATETIME not null,
	juxta_change_index float(4,3),
   alt_change_index float(4,3),
	PRIMARY KEY (id),
	FOREIGN KEY (page_id) REFERENCES pages (pg_page_id)
)  ENGINE=InnoDB DEFAULT CHARSET=utf8;

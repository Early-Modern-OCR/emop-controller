--
-- Lookup table that defines available OCR engines
--
CREATE TABLE IF NOT EXISTS ocr_engine (
   id BIGINT NOT NULL AUTO_INCREMENT,
   name varchar(20),
   PRIMARY KEY (id)
)  ENGINE=InnoDB DEFAULT CHARSET=utf8;
truncate table ocr_engine;
insert into ocr_engine (name) values ('Teseract'), ('Gamera'), ('OCROpus');

--
-- Lookup table that defines types of jobs available to be run.
-- Information about the specific OCR engine to be used in the run
-- can be found in the ocr_batch table.
--
CREATE TABLE IF NOT EXISTS job_type (
   id BIGINT NOT NULL AUTO_INCREMENT,
   name varchar(20),
   PRIMARY KEY (id)
)  ENGINE=InnoDB DEFAULT CHARSET=utf8;
truncate table job_type;
insert into job_type (name) values ('Ground Truth Compare'), ('OCR');

--
-- Lookup table to define job status
--
CREATE TABLE IF NOT EXISTS job_status (
   id BIGINT NOT NULL AUTO_INCREMENT,
   name varchar(20),
   PRIMARY KEY (id)
)  ENGINE=InnoDB DEFAULT CHARSET=utf8;
truncate table job_status;
insert into job_status (name) values ('Not Started'), ('Processing'), ('Pending Postprocess'), ('Postprocessing'), ('Done'), ('Failed');

--
-- Description of an OCR batch run. Includes version of the engine and
-- and parameters it was launched with
--
CREATE TABLE IF NOT EXISTS ocr_batch (
   id BIGINT NOT NULL AUTO_INCREMENT,
   engine_id BIGINT not null,
   parameters varchar(255),
   version varchar(50) not null,
   notes varchar(255),
   FOREIGN KEY (engine_id) REFERENCES ocr_engine (id),
   PRIMARY KEY (id)
)  ENGINE=InnoDB DEFAULT CHARSET=utf8;

--
-- Job queue
--
CREATE TABLE IF NOT EXISTS emop_job_queue (
   id BIGINT NOT NULL AUTO_INCREMENT,
   page_id int(11) NOT NULL,
   batch_id BIGINT not null,
   job_status BIGINT not null default 1,
   job_type BIGINT not null default 1,
   created TIMESTAMP not null DEFAULT CURRENT_TIMESTAMP,
   last_update TIMESTAMP,
   results varchar(255),
   PRIMARY KEY (id),
   FOREIGN KEY (page_id) REFERENCES pages (pg_page_id),
   FOREIGN KEY (batch_id) REFERENCES ocr_batch (id),
   FOREIGN KEY (job_status) REFERENCES job_status (id),
   FOREIGN KEY (job_type) REFERENCES job_status (id),
   index(page_id),
   index (batch_id)
)  ENGINE=InnoDB DEFAULT CHARSET=utf8;

--
-- Individual page results from a completed OCR run
--
CREATE TABLE IF NOT EXISTS page_results (
   id BIGINT NOT NULL AUTO_INCREMENT,
   page_id int(11) NOT NULL,
   batch_id BIGINT not null,
   ocr_text_path varchar(200) not null,
   ocr_xml_path varchar(200) not null,
   ocr_completed DATETIME not null,
   juxta_change_index float(4,3),
   alt_change_index float(4,3),
   PRIMARY KEY (id),
   FOREIGN KEY (batch_id) REFERENCES ocr_batch (id),
   FOREIGN KEY (page_id) REFERENCES pages (pg_page_id)
)  ENGINE=InnoDB DEFAULT CHARSET=utf8;

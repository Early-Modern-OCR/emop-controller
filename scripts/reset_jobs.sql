-- 
-- reset all jobs to an unstarted state
--
update job_queue set results = null, last_update='0000-00-00 00:00:00', job_status = 1;

package edu.tamu.emop;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.sql.Types;
import java.util.ArrayList;

import org.apache.log4j.Logger;

import edu.tamu.emop.model.BatchJob;
import edu.tamu.emop.model.BatchJob.OcrEngine;
import edu.tamu.emop.model.JobPage;
import edu.tamu.emop.model.JobPage.Status;
import edu.tamu.emop.model.PageInfo;
import edu.tamu.emop.model.PageInfo.OutputFormat;
import edu.tamu.emop.model.WorkInfo;

/**
 * Handles all database interactions fro the eMOP controller
 * 
 * @author loufoster
 *
 */
public class Database {
    private Connection connection;
    private static final String JOB_TABLE = "job_queue";
    private static final String BATCH_TABLE = "batch_job";
    private static final String RESULT_TABLE = "page_results";
    private static Logger LOG = Logger.getLogger(Database.class);
    
    /**
     * connect to the emop database
     * @param host
     * @param db
     * @param user
     * @param pass
     * @throws SQLException 
     */
    public void connect( final String host, final String db, final String user, final String pass ) throws SQLException {
        String connStr = "jdbc:mysql://"+host+":3306/"+db;
        this.connection = DriverManager.getConnection(connStr, user, pass);
        this.connection.setAutoCommit(false);
    }
    
    /**
     * Get the total number of pending jobs
     * @return
     * @throws SQLException
     */
    public int getJobCount() throws SQLException {
        PreparedStatement smt = null;
        ResultSet rs = null;
        try {
            final String sql = 
                "select count(id) as cnt from job_queue where job_status=?";
            smt = this.connection.prepareStatement(sql);
            smt.setLong(1, (Status.NOT_STARTED.ordinal()+1L));
            rs = smt.executeQuery();
            if (rs.first()) {
                int cnt = rs.getInt("cnt");
                return cnt;
            } else {
                this.connection.rollback();
                return 0;
            }
        } catch (SQLException e ) {
            this.connection.rollback();
            throw e;
        } finally {
            closeQuietly(rs);
            closeQuietly(smt);
        }
    }
    
    /**
     * Attempt to reserve pages for a job to OCR.  The goal is to reserve more than zero but no more than numPages pages.
     * @return
     * @throws SQLException
     */
    public int tryReservingPages(String procID, int numPages) throws SQLException {
        ArrayList<Integer> jqIDs = new ArrayList<Integer>(); //we'll use this array to keep track of jobqueue id's we want to reserve
        
        //here, we're getting a list of jobqueue id's to reserve, limited by numPages
        PreparedStatement smt = null;
        ResultSet rs = null;
        try {
            final String sql = 
                "select id from job_queue where job_status=? and proc_id is null limit ?";
            smt = this.connection.prepareStatement(sql);
            smt.setLong(1, (Status.NOT_STARTED.ordinal()+1L)); //this sets the job_status (enum value starts at zero, so we're adding one)
            smt.setInt(2, numPages); //sets the limit to numpages
            rs = smt.executeQuery();
            rs.beforeFirst();
            
            while(rs.next()) {
                jqIDs.add(rs.getInt("id"));
            }
        } catch (SQLException e ) {
            this.connection.rollback();
            throw e;
        } finally {
            closeQuietly(rs);
            closeQuietly(smt);
        }
        
        if(jqIDs.size() > 0) {
            try {
                for(int x = 0; x < jqIDs.size(); x++) { //loop through and reserve the pages by setting the unique procid
                    smt = null;
                    String sql = "update "+JOB_TABLE+" set proc_id=?, last_update=? where id=?";
                    smt = this.connection.prepareStatement(sql);
                    smt.setString(1, procID); //set the procid to variable 1
                    smt.setTimestamp(2, new Timestamp(System.currentTimeMillis()));
                    smt.setInt(3, jqIDs.get(x)); //set the jobqueue id to the current id in the array
                    smt.executeUpdate();
                    this.connection.commit();
                    closeQuietly(smt);
                }
            } catch (SQLException sqlEx) {
                
                /* THE FOLLOWING CODE HANDLES DEADLOCKING, WHICH WE'RE COMMENTING OUT FOR NOW ASSUMING NO MORE DEADLOCKING ISSUES
                // State 40001 is a deadlock. If detected, retry the update
                String sqlState = sqlEx.getSQLState();
                //if ( sqlState.equals("40001")) {
                    LOG.info("Deadlock detected when updating status of "+jobId+". Retrying...");
                    retryCount--;
                    try {
                        Thread.sleep(500) ;
                    } catch (InterruptedException e) {}
                } else {
                    // not a deadlock; bail
                */
                
                this.connection.rollback();
                throw sqlEx;
            } finally {
                closeQuietly(smt);
            }
        }
        
        /* MORE DEADLOCKING CODE THAT WE'RE COMMENTING OUT FOR NOW
        // no more retries and  transcation not complete, throw an error
        if ( retryCount == 0 && transactionComplete == false ) {
            LOG.error("No more retries for deadlock when updating status of "+jobId+".  Failing update.");
            throw new SQLException("Deadlock when updating status of "+jobId);
        }
        */
        return jqIDs.size();
    }
    
    /**
     * Get reserved jobs from the work queue. The unit of work for all jobs is
     * one page. Each page is related to a parent batch.
     * @param procID
     * @return
     * @throws SQLException 
     */
    public ArrayList<JobPage> getJobs(String procID) throws SQLException {
        ArrayList<JobPage> jobs = new ArrayList<JobPage>();
        PreparedStatement smt = null;
        ResultSet rs = null;
        try {
            // get the jobs that have been reserved for this procid
            final String sql = 
                "select job_queue.id, page_id, batch_id, job_status, created, font_name "+
                " from job_queue" + //JOB_TABLE
                " inner join batch_job on job_queue.batch_id = batch_job.id" +
                " left outer join fonts on batch_job.font_id = fonts.font_id" +
                " where job_status=? and job_queue.proc_id=? order by created ASC";
            
            smt = this.connection.prepareStatement(sql);
            smt.setLong(1, (Status.NOT_STARTED.ordinal()+1L));
            smt.setString(2, procID);
            rs = smt.executeQuery();
            rs.beforeFirst();
            
            while(rs.next()) {
                // Create the job and mark it as started. This releases the lock
                JobPage job = new JobPage();
                Long batchId = rs.getLong("batch_id") ;
                job.setId( rs.getLong("id") );
                job.setPageId( rs.getLong("page_id") );
                job.setStatus( rs.getLong("job_status") );
                job.setCreated( rs.getDate("created") );
                job.setTrainingFont( rs.getString("font_name"));
                // now pull the batch that this page is a part of
                BatchJob batch = getBatch(batchId);
                job.setBatch(batch);
                jobs.add(job);
            }
        } catch (SQLException e ) {
            this.connection.rollback();
            throw e;
        } finally {
            closeQuietly(rs);
            closeQuietly(smt);
        }
        
        return jobs;
    }
    
    /**
     * Update the status of the specified job
     * @param jobId
     * @param jobStatus
     * @throws SQLException 
     */
    public void updateJobStatus(Long jobId, Status jobStatus ) throws SQLException {
        updateJobStatus(jobId, jobStatus, null);
    }
    public void updateJobStatus(Long jobId, Status jobStatus, String result ) throws SQLException {
        PreparedStatement smt = null;
        int retryCount = 5;
        boolean transactionComplete = false;
        do {
            try {
                String sql = "update "+JOB_TABLE+" set last_update=?, job_status=?";
                if ( result != null) {
                    sql += ", results=?";
                }
                sql += " where id=?";
                smt = this.connection.prepareStatement(sql);
                smt.setTimestamp(1, new Timestamp(System.currentTimeMillis())); 
                smt.setLong(2, (jobStatus.ordinal()+1L) ); 
                if ( result != null) {
                    smt.setString(3, result);
                    smt.setLong(4, jobId);
                } else {
                    smt.setLong(3, jobId);
                }
                smt.executeUpdate();
                this.connection.commit();
                transactionComplete = true;
            } catch (SQLException sqlEx) {
                // State 40001 is a deadlock. If detected, retry the update
                String sqlState = sqlEx.getSQLState();
                if ( sqlState.equals("40001")) {
                    LOG.info("Deadlock detected when updating status of "+jobId+". Retrying...");
                    retryCount--;
                    try {
                        Thread.sleep(500) ;
                    } catch (InterruptedException e) {}
                } else {
                    // not a deadlock; bail
                    this.connection.rollback();
                    throw sqlEx;
                }
            } finally {
                closeQuietly(smt);
            }
        } while ( transactionComplete == false && retryCount > 0 );
        
        // no more retries and  transcation not complete, throw an error
        if ( retryCount == 0 && transactionComplete == false ) {
            LOG.error("No more retries for deadlock when updating status of "+jobId+".  Failing update.");
            throw new SQLException("Deadlock when updating status of "+jobId);
        }
    }
    
    /**
     * Get details about a work
     * 
     * @param pageId
     * @return
     * @throws SQLException 
     */
    public WorkInfo getWorkInfo(Long id ) throws SQLException {
        PreparedStatement smt = null;
        ResultSet rs = null;
        try {
            String sql = "select  wks_work_id, wks_title, wks_organizational_unit,wks_eebo_directory,wks_ecco_directory,wks_ecco_number from works where wks_work_id=?";
            smt = this.connection.prepareStatement(sql);
            smt.setLong(1, id ); 
            rs = smt.executeQuery();
            rs.first();
            WorkInfo work = new WorkInfo();
            work.setId( rs.getLong("wks_work_id"));
            work.setTitle( rs.getString("wks_title"));
            work.setOrganizationalUnit(rs.getLong("wks_organizational_unit"));
            work.setEccoNumber(rs.getString("wks_ecco_number"));
            work.setEccoDirectory(rs.getString("wks_ecco_directory"));
            work.setEeboDirectory(rs.getString("wks_eebo_directory"));
            return work;
        } finally {
            closeQuietly(rs);
            closeQuietly(smt);
        }
    }
    
    /**
     * Get details about a page
     * 
     * @param pageId
     * @return
     * @throws SQLException 
     */
    public PageInfo getPageInfo(Long id ) throws SQLException {
        PreparedStatement smt = null;
        ResultSet rs = null;
        try {
            String sql = "select  pg_page_id, pg_work_id, pg_ref_number,pg_ground_truth_file,pg_gale_ocr_file,pg_image_path from pages where pg_page_id=?";
            smt = this.connection.prepareStatement(sql);
            smt.setLong(1, id ); 
            rs = smt.executeQuery();
            rs.first();
            PageInfo page = new PageInfo();
            page.setId( rs.getLong("pg_page_id"));
            page.setWorkId( rs.getLong("pg_work_id"));
            page.setPageNumber( rs.getInt("pg_ref_number"));
            page.setGroundTruthFile(rs.getString("pg_ground_truth_file"));
            page.setGaleTextFile(rs.getString("pg_gale_ocr_file"));
            page.setPageImage(rs.getString("pg_image_path"));
            
            return page;
        } finally {
            closeQuietly(rs);
            closeQuietly(smt);
        }
    }
    
    /**
     * Get the path to the OCR'd text from an OCR engine for the specied pageID. This will always
     * work on the latest available version of OCR data in the history. Exception to this rule
     * is the Gale OCR. There is only one version of this, and the path to this file
     * is found in the pages table
     * 
     * @param pageId
     * @param ocrEngine 
     * @return
     * @throws SQLException 
     */
    public String getPageOcrResult(Long pageId, OcrEngine ocrEngine, OutputFormat fmt) throws SQLException {
        if ( ocrEngine.equals(OcrEngine.GALE)) {
            return getGaleOcrPageText(pageId);
        }
        
        PreparedStatement smt = null;
        ResultSet rs = null;
        try {
            final String sql = "select ocr_text_path,ocr_xml_path from page_results " +
            		"inner join batch_job on batch_id=batch_job.id " +
            		"where batch_job.ocr_engine_id=? and page_id=? order by ocr_completed desc limit 1;";
            smt = this.connection.prepareStatement(sql);
            smt.setLong(1, ocrEngine.ordinal()+1);
            smt.setLong(2, pageId);
            rs = smt.executeQuery();
            rs.first();
            if ( fmt.equals(OutputFormat.XML)) {
                return rs.getString("ocr_xml_path");
            }
            return rs.getString("ocr_text_path");
        } catch (SQLException e ) {
            throw e;
        } finally {
            closeQuietly(rs);
            closeQuietly(smt);
        }
    }
        
    private String getGaleOcrPageText(Long pageId) throws SQLException {
        // this is the simple case for retrieving OCR page text.
        PreparedStatement smt = null;
        ResultSet rs = null;
        try {
            final String sql = 
                "select works.wks_ecco_number as ecco, pg_ref_number as page_num" +
            	" from pages inner join works on pg_work_id = wks_work_id where pg_page_id=?";
            smt = this.connection.prepareStatement(sql);
            smt.setLong(1, pageId);
            rs = smt.executeQuery();
            if (rs.first()) {
                String ecco = rs.getString("ecco");
                int pageNum = rs.getInt("page_num");
                
                // target path:
                // /data/shared/text-xml/ECCO-Gale-page-OCR/[works.wks_ecco_number]/[formatted-page-number].txt
                // 0+3digit page+0.txt
                return String.format("/data/shared/text-xml/ECCO-Gale-page-OCR/%s/%04d0.txt", ecco,pageNum);
                
            } else {
                throw new RuntimeException("Unable to find Gale page results");
            }
        } catch (SQLException e ) {
            throw e;
        } finally {
            closeQuietly(rs);
            closeQuietly(smt);
        }
    }
    
    /**
     * Add OCR page results
     * 
     * @param job
     * @param parseFloat
     * @param f
     * @throws SQLException 
     */
    public void addPageResult(JobPage job, String ocrTxtFile, String ocrXmlFile, float juxtaChangeIndex, float altChangeIndex) throws SQLException {        
        // protect against bad data from comparisons
        if ( Float.isNaN(juxtaChangeIndex) ) {
            juxtaChangeIndex = 0.0f;
        }
        if ( Float.isNaN(altChangeIndex) ) {
            altChangeIndex = 0.0f;
        }
        
        PreparedStatement smt = null;
        try {
            final String sql = 
                "insert into " + RESULT_TABLE 
                + " (page_id,batch_id,ocr_text_path,ocr_xml_path,ocr_completed,juxta_change_index,alt_change_index)"
                + " values (?, ?, ?, ?, ?, ?, ?)";
            smt = this.connection.prepareStatement(sql);
            smt.setLong(1, job.getPageId());
            smt.setLong(2, job.getBatch().getId() );
            smt.setString(3, ocrTxtFile);
            smt.setString(4, ocrXmlFile);
            smt.setTimestamp(5, new Timestamp(System.currentTimeMillis())); 
            if ( juxtaChangeIndex < 0) {
                smt.setNull(6, Types.FLOAT);
            } else {
                smt.setFloat(6, juxtaChangeIndex);
            }
            if ( altChangeIndex < 0) {
                smt.setNull(7, Types.FLOAT);
            } else {
                smt.setFloat(7, altChangeIndex);
            }
            smt.executeUpdate();
            this.connection.commit();
        } finally {
            closeQuietly(smt);
        }
    }
    
    /**
     * Check for reserved or processing jobs that are older than the kill time. Restart them
     * @param killTimeSec
     * @throws SQLException 
     */
    public void restartStalledJobs(long killTimeSec) throws SQLException {
        //this update statement searches for jobs that have been processing longer than killTimeSec and frees them while incrementing number of tries
        PreparedStatement smt = null;
        try {
            final String sql = 
                "update " + JOB_TABLE 
                +" set job_status=?, last_update=?, results=?, proc_id='', tries=tries+1 where job_status=? and tries < 4 and last_update < date_sub(now(),interval "
                +killTimeSec+" second)";
            smt = this.connection.prepareStatement(sql);
            smt.setLong(1, (Status.NOT_STARTED.ordinal()+1L));
            smt.setTimestamp(2, new Timestamp(System.currentTimeMillis())); 
            smt.setString(3, "Restarted...");
            smt.setLong(4, (Status.PROCESSING.ordinal()+1L));
            smt.executeUpdate();
            this.connection.commit();
        } finally {
            closeQuietly(smt);
        }
        
        //this update statement marks jobs that have stalled more than 3 times as FAILED.
        smt = null;
        try {
            final String sql = 
                "update " + JOB_TABLE 
                +" set job_status=?, last_update=?, results=? where tries > 3 and job_status !=?";
            smt = this.connection.prepareStatement(sql);
            smt.setLong(1, (Status.FAILED.ordinal()+1L));
            smt.setTimestamp(2, new Timestamp(System.currentTimeMillis())); 
            smt.setString(3, "Failed: stalled.");
            smt.setLong(4, (Status.FAILED.ordinal()+1L));
            smt.executeUpdate();
            this.connection.commit();
        } finally {
            closeQuietly(smt);
        }
        
        //this update statement searches for jobs that have been reserved and not started for longer than 1 hour and frees them
        smt = null;
        try {
            final String sql = 
                "update " + JOB_TABLE 
                +" set last_update=?, results=?, proc_id = NULL where job_status=? and proc_id is not NULL and tries < 4 and last_update < date_sub(now(),interval "
                +"3600 second)";
            smt = this.connection.prepareStatement(sql);
            smt.setTimestamp(1, new Timestamp(System.currentTimeMillis())); 
            smt.setString(2, "Released...");
            smt.setLong(3, (Status.NOT_STARTED.ordinal()+1L));
            smt.executeUpdate();
            this.connection.commit();
        } finally {
            closeQuietly(smt);
        }
    }
    
    private BatchJob getBatch( Long id ) throws SQLException {
        PreparedStatement smt = null;
        ResultSet rs = null;
        try {
            final String sql = "select id, job_type, ocr_engine_id, parameters, name, notes" +
                    " from "+BATCH_TABLE+" where id=?";
            smt = this.connection.prepareStatement(sql);
            smt.setLong(1, id);
            rs = smt.executeQuery();
            if (rs.first()) {
                BatchJob batch = new BatchJob();
                batch.setId( rs.getLong("id") );
                batch.setJobType( rs.getLong("job_type") );
                batch.setOcrEngine( rs.getLong("ocr_engine_id") );
                batch.setParameters( rs.getString("parameters") );
                batch.setName( rs.getString("name") );
                batch.setNotes( rs.getString("notes") );
                return batch;
            } else {
                return null;
            }
        } finally {
            closeQuietly(rs);
            closeQuietly(smt);
        }
    }
    
    private void closeQuietly(Statement smt ) {
        if ( smt != null ) {
            try {
                smt.close();
            } catch (SQLException e) {
            }
        }
    }
    
    private void closeQuietly(ResultSet rs ) {
        if ( rs != null ) {
            try {
                rs.close();
            } catch (SQLException e) {
            }
        }
    }
    
    /**
     * Terminate DB connection
     */
    public void disconnect() {
        try {
            this.connection.close();
        } catch (SQLException e) {
        }
    }
}

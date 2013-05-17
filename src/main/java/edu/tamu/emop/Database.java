package edu.tamu.emop;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;

import edu.tamu.emop.model.BatchJob.OcrEngine;
import edu.tamu.emop.model.JobPage;
import edu.tamu.emop.model.JobPage.Status;
import edu.tamu.emop.model.BatchJob;

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
     * Get an active job from the head of the work queue. The unit of work for all jobs is
     * one page. Each page is related to a parent batch.
     * 
     * @return
     * @throws SQLException 
     */
    public JobPage getJob() throws SQLException {
        PreparedStatement smt = null;
        ResultSet rs = null;
        try {
            // Note the 'for update' at the end. This locks the row so others cannot access
            // it during this process. The lock is released when a job is found and its
            // status is marked as STARTED. 
            final String sql = "select id, page_id, batch_id, job_status, created" +
            		" from "+JOB_TABLE+" where job_status=? order by created ASC limit 1 for update";
            smt = this.connection.prepareStatement(sql);
            smt.setLong(1, (Status.NOT_STARTED.ordinal()+1L));
            rs = smt.executeQuery();
            if (rs.first()) {
                // Create the job and mark it as started. This releases the lock
                JobPage job = new JobPage();
                Long batchId = rs.getLong("batch_id") ;
                job.setId( rs.getLong("id") );
                job.setPageId( rs.getLong("page_id") );
                job.setStatus( rs.getLong("job_status") );
                job.setCreated( rs.getDate("created") );
                updateJobStatus(job.getId(), Status.PROCESSING);
                
                // now pull the batch that this page is a part of
                BatchJob batch = getBatch(batchId);
                job.setBatch(batch);
                return job;
            } else {
                this.connection.rollback();
                return null;
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
        } catch (SQLException e) {
            this.connection.rollback();
            throw e;
        } finally {
            closeQuietly(smt);
        }
    }
    
    /**
     * Get edition page number for a specific page id
     * 
     * @param pageId
     * @param ocrEngine
     * @return
     * @throws SQLException 
     */
    public int getPageNumber( Long pageId ) throws SQLException {
        PreparedStatement smt = null;
        ResultSet rs = null;
        try {
            String sql = "select  pg_ref_number from pages where pg_page_id=?";
            smt = this.connection.prepareStatement(sql);
            smt.setLong(1, pageId ); 
            rs = smt.executeQuery();
            rs.first();
            return rs.getInt("pg_ref_number");
        } catch (SQLException e) {
            throw e;
        } finally {
            closeQuietly(rs);
            closeQuietly(smt);
        }
    }
    
    /**
     * Get the path to the ground truth file for the specified pageID
     * @param pageId
     * @return
     * @throws SQLException 
     */
    public String getPageGroundTruth(Long pageId) throws SQLException {
        PreparedStatement smt = null;
        ResultSet rs = null;
        try {
            final String sql = "select pg_ground_truth_file from pages where pg_page_id=?";
            smt = this.connection.prepareStatement(sql);
            smt.setLong(1, pageId);
            rs = smt.executeQuery();
            if (rs.first()) {
                return rs.getString("pg_ground_truth_file");
            } else {
                return "";
            }
        } catch (SQLException e ) {
            throw e;
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
    public String getPageOcrText(Long pageId, OcrEngine ocrEngine) throws SQLException {
        if ( ocrEngine.equals(OcrEngine.GALE)) {
            return getGaleOcrPageText(pageId);
        }
        // FIXME not impemented
        return "";
    }
    
    private String getGaleOcrPageText(Long pageId) throws SQLException {
        // this is the simple case for retrieving OCR page text.
        // Gale has only one version, and the path is found in 
        // the pages table.
        PreparedStatement smt = null;
        ResultSet rs = null;
        try {
            final String sql = "select pg_gale_text_file from pages where pg_page_id=?";
            smt = this.connection.prepareStatement(sql);
            smt.setLong(1, pageId);
            rs = smt.executeQuery();
            if (rs.first()) {
                return rs.getString("pg_gale_text_file");
            } else {
                return "";
            }
        } catch (SQLException e ) {
            throw e;
        } finally {
            closeQuietly(rs);
            closeQuietly(smt);
        }
    }

    /**
     * Get the path to the page image for the specified pageID
     * @param pageId
     * @return
     * @throws SQLException 
     */
    public String getPageImage(Long pageId) throws SQLException {
        PreparedStatement smt = null;
        ResultSet rs = null;
        try {
            final String sql = 
                "select wks_eebo_directory,wks_ecco_directory,wks_ecco_number,pg_ref_number from works inner join pages on pg_work_id=wks_work_id where pg_page_id=?";
            smt = this.connection.prepareStatement(sql);
            smt.setLong(1, pageId);
            rs = smt.executeQuery();
            if (rs.first()) {
                Object obj = rs.getObject("wks_ecco_directory");
                if ( obj != null ) {
                    // ECCO format: ECCO number + 4 digit page + 0.tif
                    return String.format("%s/images/%s%04d0.TIF", rs.getString("wks_ecco_directory"), rs.getString("wks_ecco_number"), rs.getInt("pg_ref_number") );
                } else {
                    obj = rs.getObject("wks_eebo_directory");
                    if ( obj != null ) {
                        // EEBO format: 00014.000.001.tif where 00014 is the page number.
                        return String.format("%s/%05d.000.001.tif", rs.getString("wks_eebo_directory"), rs.getInt("pg_ref_number") );
                    }
                    return "";
                }
            } else {
                return "";
            }
        } catch (SQLException e ) {
            throw e;
        } finally {
            closeQuietly(rs);
            closeQuietly(smt);
        }
    }
    
    /**
     * Check for processing jobs that are older than the kill time. Mark them
     * as failed with a reason of timed out
     * @param killTimeSec
     * @throws SQLException 
     */
    public void failStalledJobs(long killTimeSec) throws SQLException {
        PreparedStatement smt = null;
        try {
            final String sql = 
                "update " + JOB_TABLE 
                +" set job_status=?, last_update=?, results=? where job_status=? and last_update < date_sub(now(),interval "
                +killTimeSec+" second)";
            smt = this.connection.prepareStatement(sql);
            smt.setLong(1, (Status.FAILED.ordinal()+1L));
            smt.setTimestamp(2, new Timestamp(System.currentTimeMillis())); 
            smt.setString(3, "Timed Out");
            smt.setLong(4, (Status.PROCESSING.ordinal()+1L));
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

package edu.tamu.emop;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import edu.tamu.emop.model.EmopJob;
import edu.tamu.emop.model.EmopJob.Status;
import edu.tamu.emop.model.OcrBatch;

/**
 * Handles all database interactions fro the eMOP controller
 * 
 * @author loufoster
 *
 */
public class Database {
    private Connection connection;
    private static final String JOB_TABLE = "emop_job_queue";
    private static final String BATCH_TABLE = "ocr_batch";
    
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
     * Get an active job from the head of the work queue
     * @return
     * @throws SQLException 
     */
    public EmopJob getJob() throws SQLException {
        PreparedStatement smt = null;
        ResultSet rs = null;
        try {
            // Note the 'for update' at the end. This locks the row so others cannot access
            // it during this process. The lock is released when a job is found and its
            // status is marked as STARTED. 
            final String sql = "select id, page_id, batch_id, job_status, job_type, created" +
            		" from "+JOB_TABLE+" where job_status=? order by created ASC limit 1 for update";
            smt = this.connection.prepareStatement(sql);
            smt.setLong(1, (Status.NOT_STARTED.ordinal()+1L));
            rs = smt.executeQuery();
            if (rs.first()) {
                // grab the job and mark it as started
                EmopJob job = jobFromRs(rs);
                updateJobStatus(job.getId(), Status.PROCESSING);
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
            String sql = "update "+JOB_TABLE+" set job_status=?";
            if ( result != null) {
                sql += ", results=?";
            }
            sql += " where id=?";
            smt = this.connection.prepareStatement(sql);
            smt.setLong(1, (jobStatus.ordinal()+1L) ); 
            if ( result != null) {
                smt.setString(2, result);
                smt.setLong(3, jobId);
                
            } else {
                smt.setLong(2, jobId);
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
     * Get the path to the OCR'd text for the specied pageID
     * @param pageId
     * @return
     */
    public String getPageOcrText(Long pageId) {
        return "";
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
                        // EEBO format: 00014.000.001.tif
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
     * Get details about the specified OCR Batch ids
     * 
     * @param id
     * @return
     * @throws SQLException
     */
    public OcrBatch getBatch( Long id ) throws SQLException {
        PreparedStatement smt = null;
        ResultSet rs = null;
        try {
            final String sql = "select id, engine_id, parameters, version, notes" +
                    " from "+BATCH_TABLE+" where id=?";
            smt = this.connection.prepareStatement(sql);
            smt.setLong(1, (Status.NOT_STARTED.ordinal()+1L));
            rs = smt.executeQuery();
            if (rs.first()) {
                return batchFromRs(rs);
            } else {
                return null;
            }
        } finally {
            closeQuietly(rs);
            closeQuietly(smt);
        }
    }
    
    private OcrBatch batchFromRs( ResultSet rs) throws SQLException {
        OcrBatch batch = new OcrBatch();
        batch.setId( rs.getLong("id") );
        batch.setOcrEngine( rs.getLong("engine_id") );
        batch.setParameters( rs.getString("parameters") );
        batch.setVersion( rs.getString("version") );
        batch.setNotes( rs.getString("notes") );
        return batch;
    }
    
    private EmopJob jobFromRs( ResultSet rs) throws SQLException {
        EmopJob job = new EmopJob();
        job.setId( rs.getLong("id") );
        job.setPageId( rs.getLong("page_id") );
        job.setBatchId( rs.getLong("batch_id") );
        job.setStatus( rs.getLong("job_status") );
        job.setJobType( rs.getLong("job_type") );
        job.setCreated( rs.getDate("created") );
        return job;
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

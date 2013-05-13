package edu.tamu.emop;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import edu.tamu.emop.model.EmopJob;
import edu.tamu.emop.model.EmopJob.Status;


public class Database {
    private Connection connection;
    private static final String JOB_TABLE = "emop_job_queue";
    
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
            final String sql = "select id, page_id, batch_id, job_status, job_type, created" +
            		" from "+JOB_TABLE+" where job_status=? order by created ASC limit 1 for update";
            smt = this.connection.prepareStatement(sql);
            smt.setLong(1, (Status.NOT_STARTED.ordinal()+1L));
            rs = smt.executeQuery();
            if (rs.first()) {
                return jobFromRs(rs);
            } else {
                return null;
            }
        } finally {
            closeQuietly(rs);
            closeQuietly(smt);
        }
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

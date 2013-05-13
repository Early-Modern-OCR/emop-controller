package edu.tamu.emop;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.sql.SQLException;
import java.util.Properties;

import org.apache.commons.io.IOUtils;
import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.Level;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;
import org.apache.log4j.RollingFileAppender;

import edu.tamu.emop.model.EmopJob;

/**
 * eMOP controller app. Responsible for pulling jobs from the work
 * queue in the emop database and servicing them. Updates job status
 * and writes results to output directory.
 * 
 * This process depends upon environment variables and .my.cnf to execute properly.
 * Expected environment:
 *    EMOP_WALLTIME
 *    JUXTA_HOME
 *    EMOP_RESULTS_DIR
 * 
 * @author loufoster
 *
 */
public class EmopController {
    private Database db;
    private int timeLeftMs;
    private String juxtaHome;
    private String resultsRoot;
    private static Logger LOG = Logger.getLogger(EmopController.class);

    public static void main(String[] args) {
        boolean consoleLog =  ( args.length == 1 && args[0].equals("-console") );
        try {
            initLogging( consoleLog );

            EmopController emop = new EmopController();
            emop.init();
            emop.doWork();
            emop.shutdown();
        } catch ( SQLException e) {
            System.err.println("eMOP Controller database error");
            e.printStackTrace();
            System.exit(-1);
        } catch ( Exception e) {
            System.err.println("eMOP Controller FAILED");
            e.printStackTrace();
            System.exit(-1);
        }
    }
    
    protected static void initLogging( boolean consoleLogger ) throws IOException {
        LogManager.getRootLogger().removeAllAppenders();
        LogManager.getRootLogger().setLevel(Level.DEBUG);
        if ( consoleLogger == false ) {
            RollingFileAppender fileApp = new RollingFileAppender(new PatternLayout("%d{E MMM dd, HH:mm:ss} [%p] - %m%n"), "log/emop.log", true);
            fileApp.setThreshold(Level.DEBUG);
            fileApp.activateOptions();
            LogManager.getRootLogger().addAppender(fileApp);
        } else {
            ConsoleAppender console = new ConsoleAppender(new PatternLayout("%m%n")); 
            console.setThreshold(Level.DEBUG);
            console.activateOptions();
            LogManager.getRootLogger().addAppender(console);
        }
    }
    
    /**
     * Initialize the controller. Get run settings from environment. Init database connection.
     * @throws SQLException 
     * @throws FileNotFoundException 
     */
    public void init() throws IOException, SQLException {
        LOG.info("Initialize eMOP controller");
        
        // pull settings .my.cf
        Properties mySql = loadMySqlCfg();
        
        String dbHost = mySql.getProperty("host");
        if ( dbHost == null || dbHost.length() == 0) {
            dbHost = "localhost";
        }
        
        String dbUser =   mySql.getProperty("user");
        if ( dbUser == null || dbUser.length() == 0) {
            dbUser = "root";
        }
        
        String dbPass = mySql.getProperty("pass");
        if ( dbPass == null || dbPass.length() == 0) {
            dbPass = "";
        }
        
        String strWorkTimeSec =  System.getenv("EMOP_WALLTIME");
        this.timeLeftMs = 5*1000;
        if ( strWorkTimeSec != null && strWorkTimeSec.length() > 0) {
            this.timeLeftMs = Integer.parseInt(strWorkTimeSec);
        }
        
        this.juxtaHome =  System.getenv("JUXTA_HOME");
        if ( this.juxtaHome == null || this.juxtaHome.length() == 0) {
            throw new RuntimeException("Missing require JUXTA_HOME environment variable");
        }
        
        this.resultsRoot =  System.getenv("EMOP_RESULTS_DIR");
        if ( this.resultsRoot == null || this.resultsRoot.length() == 0) {
            throw new RuntimeException("Missing require EMOP_RESULTS_DIR environment variable");
        }

        this.db = new Database();
        this.db.connect(dbHost, "emop", dbUser, dbPass);
    }
    
    private Properties loadMySqlCfg() throws IOException {
        File home = new File(System.getenv("HOME"));
        File myCnf = new File(home, ".my.cnf");
        FileInputStream fis = new FileInputStream(myCnf );
        Properties mySqlProp = new Properties();
        mySqlProp.load(fis);
        IOUtils.closeQuietly(fis);
        return mySqlProp;
    }

    /**
     * Main work look for the controller. As long as time remains, pick off availble jobs.
     * Mark the as in-process and kick off a task to service them. When complete, 
     * record data to configured out location and mark task as pending post-processing.
     * @throws SQLException 
     */
    public void doWork() throws SQLException {
        do {
            long t0 = System.currentTimeMillis();
            EmopJob job = this.db.getJob();
            if ( job == null ) {
                LOG.info("No jobs to process. Terminating.");
                break;
            }
            try {
                Thread.sleep(1000);
            } catch (Exception e ) {}
            
            this.timeLeftMs -= ( (System.currentTimeMillis()-t0) );
        } while ( this.timeLeftMs > 0);
    }
    
    /**
     * Shutdown the emop controller and release any allocated resources
     */
    public void shutdown() {
        LOG.info("Shutdown eMOP controller");
        if ( this.db != null ) {
            this.db.disconnect();
        }
    }
}

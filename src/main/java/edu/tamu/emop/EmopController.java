package edu.tamu.emop;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.sql.SQLException;
import java.util.Properties;
import java.util.Set;
import java.util.TreeSet;

import javax.xml.transform.TransformerException;

import org.apache.commons.cli2.Argument;
import org.apache.commons.cli2.CommandLine;
import org.apache.commons.cli2.Group;
import org.apache.commons.cli2.Option;
import org.apache.commons.cli2.OptionException;
import org.apache.commons.cli2.builder.ArgumentBuilder;
import org.apache.commons.cli2.builder.DefaultOptionBuilder;
import org.apache.commons.cli2.builder.GroupBuilder;
import org.apache.commons.cli2.commandline.Parser;
import org.apache.commons.cli2.validation.EnumValidator;
import org.apache.commons.io.IOUtils;
import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.Level;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;
import org.apache.log4j.RollingFileAppender;
import org.xml.sax.SAXException;

import edu.tamu.emop.model.BatchJob;
import edu.tamu.emop.model.BatchJob.JobType;
import edu.tamu.emop.model.BatchJob.OcrEngine;
import edu.tamu.emop.model.JobPage;
import edu.tamu.emop.model.JobPage.Status;
import edu.tamu.emop.model.PageInfo;
import edu.tamu.emop.model.PageInfo.OutputFormat;
import edu.tamu.emop.model.WorkInfo;

/**
 * eMOP controller app. Responsible for pulling jobs from the work
 * queue in the emop database and servicing them. Updates job status
 * and writes results to output directory.
 * 
 * This process depends upon environment variables and .my.cnf to execute properly.
 * Expected environment:
 *    JUXTA_HOME         - base directory of juxtaCL install
 *    RETAS_HOME         - base directory of RETAS install
 *    
 * Optional Environment:
 *    TESSERACT_HOME     - base directory of tesseract install

 *    
 * @author loufoster
 *
 */
public class EmopController {
    public enum Algorithm {JUXTA, LEVENSHTEIN, JARO_WINKLER};
    
    private Database db;
    private long timeLeftMs;
    private long wallTimeSec;
    private String juxtaHome;
    private String retasHome;
    private String tesseractHome = "";
    private String pathPrefix = "";
    private Algorithm algorithm = Algorithm.JARO_WINKLER;
    private HocrTransformer hocrTransformer;
    
    private static Logger LOG = Logger.getLogger(EmopController.class);
    private static final long JX_TIMEOUT_MS = 1000*60*2;    //2 mins
    
    private static final String version = "1.0-SNAPSHOT";
    
    /**
     * Main entry point for the controller
     * @param args
     * @throws Exception 
     */
    public static void main(String[] args) throws Exception {   
        System.setProperty("javax.xml.transform.TransformerFactory", "net.sf.saxon.TransformerFactoryImpl");
        
        try {
            EmopController emop = new EmopController();
            if ( emop.init( args ) ) {
                emop.killStalledJobs();
                emop.doWork();
                emop.shutdown();
            }
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

    /**
     * Initialize controller logging. In debug mode, just write all logs out to std:out
     * @param consoleLogger
     * @throws IOException
     */
    private void initLogging( boolean consoleLogger ) throws IOException {
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
     * @throws OptionException 
     * @throws FileNotFoundException 
     */
    public boolean init( String[] args ) throws IOException, SQLException, OptionException {
                           
        // parse the commandline settings
        final ArgumentBuilder aBuilder = new ArgumentBuilder();
        final DefaultOptionBuilder oBuilder = new DefaultOptionBuilder();
        final GroupBuilder gBuilder = new GroupBuilder();
        
        Option version = oBuilder
            .withShortName("version")
            .create();
        Option help = oBuilder
            .withShortName("help")
            .create();
        Option consoleLog = oBuilder
            .withShortName("console")
            .create();
        
        // out directory
        Argument outArg = aBuilder.withMinimum(1).withMaximum(1).withName("path").create();
        Option out = oBuilder
            .withShortName("out")
            .withArgument(outArg )
            .create();
        
        // algorithm
        Set<String> diffSet = new TreeSet<String>();
        diffSet.add("juxta");
        diffSet.add("levenshtein");
        diffSet.add("jaro_winkler");
        EnumValidator algoVal = new EnumValidator( diffSet );
        Option algo = oBuilder
          .withShortName("algorithm")
          .withArgument(
              aBuilder
                  .withMinimum(0)
                  .withMaximum(1)
                  .withDefault("juxta")
                  .withValidator(algoVal)
                  .create())
          .create();
        
        Group jobOpts = gBuilder.withOption(consoleLog).withOption(out).withOption(algo).create(); 
        
        // main param: job wall time
        Argument wallTimeArg = aBuilder.withMinimum(1).withMaximum(1).withName("time").create();
        Option job = oBuilder
            .withShortName("wall")
            .withArgument(wallTimeArg )
            .withChildren(jobOpts)
            .create();

        
        // glom all of the options together into the main grouping. it will be used for the parse.
        Group opts = gBuilder
            .withOption(job)
            .withOption(help)
            .withOption(version).create();

        // parse the options passed in
        Parser parser = new Parser();
        parser.setGroup(opts);
        CommandLine cl = parser.parse(args);
       
        if ( cl.hasOption(version)) {
            System.out.println("eMOP Controller Version "+EmopController.version);
            return false;
        } 
        
        if ( cl.hasOption(help)) { 
            showHelp();
            return false;
        } 
        
        boolean logToConsole = cl.hasOption(consoleLog);
        initLogging(logToConsole) ;
        
        if ( cl.hasOption(algo)) {
            String a = (String)cl.getValue(algo);
            this.algorithm = Algorithm.valueOf(a.toUpperCase());
        }
        
        if ( cl.hasOption(job)) {
            String timeStr = (String)cl.getValue(job);
            if ( timeStr != null && timeStr.length() > 0 ) {
                this.wallTimeSec = Integer.parseInt(timeStr);
            }
        }        
        this.timeLeftMs = this.wallTimeSec*1000;

        if ( cl.hasOption(out)) {
            String outVal = (String)cl.getValue(out);
            if ( outVal != null && outVal.length() > 0 ) {
                this.pathPrefix = outVal;
            }
        }
        
        // pull some cfg from environment settings
        getEnvironmentConfig();

        LOG.info("Initialize eMOP controller");
        initDatabase();
        return true;
    }
    
    private void getEnvironmentConfig() {
        this.juxtaHome =  System.getenv("JUXTA_HOME");
        if ( this.juxtaHome == null || this.juxtaHome.length() == 0) {
            throw new RuntimeException("Missing require JUXTA_HOME environment variable");
        }
        
        this.retasHome =  System.getenv("RETAS_HOME");
        if ( this.retasHome == null || this.retasHome.length() == 0) {
            throw new RuntimeException("Missing require RETAS_HOME environment variable");
        }
        
        String th =  System.getenv("TESSERACT_HOME");
        if ( th != null && th.length() > 0) {
            this.tesseractHome = th;
        }
    }

    private void showHelp() {
        StringBuilder out = new StringBuilder("Usage: emop wall [timeSec] [options]\n");
        out.append("Where wall defines process lifespan in seconds.");
        out.append("  Options:\n");
        out.append("    out        : Path to root directory for output.\n");
        out.append("    algorithm  : Algorithm used by JuxtaCL to determine change index.\n");
        out.append("                   Options:\n");
        out.append("                     juxta, levenshtein, dice_sorensen jaro_winkler\n");
        out.append("                   Defaults to jaro_winkler\n");
        System.out.println(out.toString());
    }

    private void initDatabase() throws IOException, SQLException {
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
     * Scan the job queue for jobs that have stayed in the PROCESSING state for too
     * long. Mark them as failed with a result of Timed Out.
     */
    public void killStalledJobs() {
        // if something has been in process for longer than the wall time
        // the controller that started it has been killed and the job will 
        // not complete. mark it as timed out
        try {
            this.db.failStalledJobs( this.wallTimeSec);
        } catch (SQLException e ) {
            LOG.error("Unabled to flag long-running jobs as timed out", e);
        }
    }

    /**
     * Main work look for the controller. As long as time remains, pick off availble jobs.
     * Mark the as in-process and kick off a task to service them. When complete, 
     * record data to configured out location and mark task as pending post-processing.
     * @throws SQLException 
     */
    public void doWork() throws SQLException {
        long totalMs = 0;
        do {
            long t0 = System.currentTimeMillis();
            
            // check for availble jobs; bail if none are available
            JobPage job = this.db.getJob();
            if ( job == null ) {
                LOG.info("No jobs to process. Terminating.");
                break;
            }
            
            // get details about the OCR batch that is to be
            // used for this jobs
            BatchJob batch = job.getBatch();
            LOG.info("Got job ["+job.getId()+"] - Batch: "+batch.getName()+" job Type: "+batch.getJobType()+", OCR engine: " + batch.getOcrEngine());
            if ( batch.getJobType().equals(JobType.GT_COMPARE)) {
                doGroundTruthCompare(job);
            } else {
                doOCR(job);
            }
            
            long durationMs = (System.currentTimeMillis()-t0);
            totalMs+= durationMs;
            LOG.info("Job ["+job.getId()+"] COMPLETE. Duration: "+(durationMs/1000f)+" secs");
            this.timeLeftMs -= durationMs;
        } while ( this.timeLeftMs > 0);
        LOG.info("==> TOTAL TIME: "+totalMs/1000f);
    }
    
    /**
     * Run an OCR job. If successful and GT is available, run a GT comparison and record results
     * 
     * @param job
     * @throws SQLException
     */
    private void doOCR(JobPage job) throws SQLException {
        // get details about the page and work associated with this job
        PageInfo pageInfo = this.db.getPageInfo(job.getPageId());
        WorkInfo workInfo = this.db.getWorkInfo(pageInfo.getWorkId());
        String img = workInfo.getPageImage(pageInfo.getPageNumber());
        String ocrXmlFile = workInfo.getOcrOutFile(job.getBatch(), OutputFormat.XML, pageInfo.getPageNumber());
        String ocrTxtFile = workInfo.getOcrOutFile(job.getBatch(), OutputFormat.TXT, pageInfo.getPageNumber());
        
        try {
            // call the correct engine based upon batch config
            if ( job.getBatch().getOcrEngine().equals(OcrEngine.TESSERACT)) {
                LOG.info("Using Tesseract to OCR "+img);
                doTesseractOcr( addPrefix(img), job.getBatch().getParameters(), addPrefix(ocrXmlFile) );
            } else {
                LOG.error("OCR with "+job.getBatch().getOcrEngine()+" not yet supported");
                this.db.updateJobStatus(job.getId(), Status.FAILED, job.getBatch().getOcrEngine()+" not supported");
                return;
            }
            
            // If no ground truth, we are done
            if ( pageInfo.hasGroundTruth() == false ) {
                LOG.warn("Ground truth does not exist for page "+job.getPageId());
                this.db.updateJobStatus(job.getId(), Status.PENDING_POSTPROCESS, "GT does not exist");
                return;
            }
            
            this.db.updateJobStatus(job.getId(), Status.PENDING_POSTPROCESS, "DONE");
            
            // do the GT compares
            float juxtaVal = juxtaCompare(pageInfo.getGroundTruthFile(), ocrTxtFile);
            float retasVal = retasCompare(pageInfo.getGroundTruthFile(), ocrTxtFile);
            
            // log the results
            this.db.updateJobStatus(job.getId(), Status.PENDING_POSTPROCESS, "{\"JuxtaCL\": \""+juxtaVal+"\", \"RETAS\": \""+retasVal+"\"}");
            this.db.addPageResult(job, ocrTxtFile, ocrXmlFile, juxtaVal, retasVal);
            
        } catch (InterruptedException e) {
            LOG.error("Job timed out");
            this.db.updateJobStatus(job.getId(), Status.FAILED, "Timed Out");
        } catch ( Exception e ) {
            LOG.error("Job Failed", e);
            this.db.updateJobStatus(job.getId(), Status.FAILED, e.getMessage());
        }
    }
    
    private float juxtaCompare(String gtFile, String ocrTxtFile) throws InterruptedException, IOException, SQLException {
        LOG.info("Compare OCR results with ground truth using JuxtaCL");
        String out = "";
        ProcessBuilder pb = new ProcessBuilder(
            "java", "-jar", "-server", 
            this.juxtaHome+"/juxta-cl.jar", "-diff",
            addPrefix( gtFile ), addPrefix(ocrTxtFile), 
            "-algorithm", this.algorithm.toString().toLowerCase(), "-hyphen", "none");
        pb.directory( new File(this.juxtaHome) );
        Process jxProc = pb.start();
        awaitProcess(jxProc, JX_TIMEOUT_MS);
        out = IOUtils.toString(jxProc.getInputStream());
        
        if (jxProc.exitValue() == 0) {
            return Float.parseFloat(out.trim());
        } else {
            LOG.error(out);
            throw new IOException( out);
        }
    }
    
    private float retasCompare(String gtFile, String ocrTxtFile) throws InterruptedException, IOException, SQLException {
        LOG.info("Compare OCR results with ground truth using RETAS");
        String out = "";
        ProcessBuilder pb = new ProcessBuilder(
            "java", "-jar", "-server", 
            this.retasHome+"/retas.jar",
            addPrefix( gtFile ), addPrefix(ocrTxtFile), 
            "-opt", this.retasHome+"/config.txt");
        pb.directory( new File(this.retasHome) );
        Process jxProc = pb.start();
        awaitProcess(jxProc, JX_TIMEOUT_MS);
        out = IOUtils.toString(jxProc.getInputStream());
        
        if (jxProc.exitValue() == 0) {
            // retas output is 3 tab delimited values. First 2 are the 
            // comparands, last is the result. it is all we care about
            return Float.parseFloat(out.trim().split("\t")[2]);
        } else {
            LOG.error(out);
            throw new IOException( out);
        }
    }

    /**
     * Use Tesseract to OCR the specified image file. Pass along any params from the batch
     * 
     * @param img Path to the page image
     * @param priorVersionCnt number of pre-existing ocr'd versions of this page
     * @param params (may be null) Parameters to pass along to tesseract
     * @return Name of the OCR text file
     * @throws InterruptedException
     * @throws IOException
     * @throws TransformerException 
     * @throws SAXException 
     */
    private void doTesseractOcr(String pageImage, String params, String outFile) throws InterruptedException, IOException, SAXException, TransformerException { 
        // ensure that the directory tree is present
        File out = new File(outFile);
        out.getParentFile().mkdirs();
        String exe = "tesseract";
        if ( this.tesseractHome != null  && this.tesseractHome.length() > 0 ) {
            exe  = this.tesseractHome+"/tesseract";
        }
        
        // NOTE: strip the .txt extension; tesseract auto-appends it
        final String trimmedOut = outFile.substring(0,outFile.length()-4);
        
        // kickoff the OCR engine and wait until it completes
        ProcessBuilder pb = new ProcessBuilder( exe, pageImage, trimmedOut, "hocr" );
        Process jxProc = pb.start();
        awaitProcess(jxProc, JX_TIMEOUT_MS);
        if (jxProc.exitValue() != 0) {
            String err = IOUtils.toString(jxProc.getErrorStream());
            throw new RuntimeException("OCR failed: "+err);
        }
        
        // end result is an XHTML file containing all of the work coordinates
        LOG.info("Extract TXT content from hOCR");
        if ( this.hocrTransformer == null ) {
            this.hocrTransformer = new HocrTransformer();
            this.hocrTransformer.initialize();
        }
        String txtOut = this.hocrTransformer.extractTxt(trimmedOut+".html");
        OutputStreamWriter osw = new OutputStreamWriter(new FileOutputStream(trimmedOut+".txt"), "UTF-8");
        IOUtils.write(txtOut, osw);
        osw.close();

    }

    private void doGroundTruthCompare(JobPage job) throws SQLException {  
        // get details about the page associated with this job
        PageInfo pageInfo = this.db.getPageInfo(job.getPageId());
        if ( pageInfo.hasGroundTruth() == false) {
            LOG.warn("Ground truth does not exist for page "+job.getPageId());
            this.db.updateJobStatus(job.getId(), Status.PENDING_POSTPROCESS, "GT does not exist");
            return;
        }
        
        // GT exists, grab the path
        String gtPath = pageInfo.getGroundTruthFile();
        
        // now pull page result based on OCR engine
        String ocrTxtFile = this.db.getPageOcrResult(job.getPageId(), job.getBatch().getOcrEngine(), OutputFormat.TXT);
        String ocrXmlFile = this.db.getPageOcrResult(job.getPageId(), job.getBatch().getOcrEngine(), OutputFormat.XML);
        
        try {
            // do the GT compares
            float juxtaVal = juxtaCompare( gtPath, ocrTxtFile );
            float retasVal = retasCompare(pageInfo.getGroundTruthFile(), ocrTxtFile);
            
            // log the results
            this.db.updateJobStatus(job.getId(), Status.PENDING_POSTPROCESS, "{\"JuxtaCL\": \""+juxtaVal+"\", \"RETAS\": \""+retasVal+"\"}");
            this.db.addPageResult(job, ocrTxtFile, ocrXmlFile, juxtaVal, retasVal);
            
        } catch (InterruptedException e) {
            LOG.error("Job timed out");
            this.db.updateJobStatus(job.getId(), Status.FAILED, "Timed Out");
        } catch ( Exception e ) {
            LOG.error("Job Failed", e);
            this.db.updateJobStatus(job.getId(), Status.FAILED, e.getMessage());
        }
    }
    
    public String addPrefix (String path) {
        File file1 = new File(this.pathPrefix);
        File file2 = new File(file1, path);
        return file2.getPath();
    }
    
    private void awaitProcess(Process p, long timeoutMs) throws InterruptedException {
        long now = System.currentTimeMillis();
        long killTimeMs = now + timeoutMs;
        while (isAlive(p) && (System.currentTimeMillis() < killTimeMs)) {
            try {
                Thread.sleep(10);
            } catch (InterruptedException e) {
                // no-op
            }
        }
        if (isAlive(p)) {
            throw new InterruptedException("Process timed out");
        }
    }
    
    private boolean isAlive(Process p) {
        try {
            p.exitValue();
            return false;
        } catch (IllegalThreadStateException e) {
            return true;
        }
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

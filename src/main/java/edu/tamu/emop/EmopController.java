package edu.tamu.emop;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Properties;

import javax.xml.transform.TransformerException;

import org.apache.commons.cli.BasicParser;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;
import org.apache.commons.io.IOUtils;
import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.Level;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;
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
 *    SEASR_HOME         - base directory of SEASR install
 *
 * @author loufoster
 *
 */
public class EmopController {
    public enum Algorithm {JUXTA, LEVENSHTEIN, JARO_WINKLER};
    public enum Mode {RUN, CHECK, RESERVE}

    private Database db;
    private long timeLeftMs = -1;   // run til all jobs done
    private long wallTimeSec = -1;  // run til all jobs done
    private int numPages = 0; // number of pages to reserve

    private String emopHome;
    private String juxtaHome;
    private String retasHome;
    private String seasrHome;
    private String procID; // the process ID with which to reserve or OCR pages

    private String pathPrefix = "";
    private Algorithm algorithm = Algorithm.JARO_WINKLER;

    private static Logger LOG = Logger.getLogger(EmopController.class);
    private static final long JX_TIMEOUT_MS = 1000*60*10;    //10 mins

    /**
     * Main entry point for the controller
     * @param args
     * @throws Exception
     */
    public static void main(String[] args) throws Exception {
        System.setProperty("javax.xml.transform.TransformerFactory", "net.sf.saxon.TransformerFactoryImpl");

        Mode mode = Mode.RUN; // set the defailt operating mode
        String procID = "";
        int numPages = 0;

        // set up the available commandline options
        Options cliOptions = new Options();
        cliOptions.addOption( OptionBuilder.withDescription("Available options for mode are: run, reserve, and check.\r\n"
                                    + "run: This mode kicks off the OCR job on reserved pages, and requires one additional parameter: the Process ID [procid], which tells the job which pages to OCR.\r\n"
                                    + "reserve: This mode reserves a number of pages to be OCR'ed in the future given a Process ID, and requires two parameters: the Process ID [procid] to be associated with the pages, and the number of pages [numpages] to be OCR'ed.\r\n"
                                    + "check: This mode checks for the number of pages in the job queue, and requires no parameters.")
                                .hasArg()
                                .withArgName("RUNMODE")
                                .create("mode") );
        cliOptions.addOption( OptionBuilder.withDescription("Process ID for reserving pages or running a job.")
                                .hasArg()
                                .withArgName("PROCESSID")
                                .create("procid") );
        cliOptions.addOption( OptionBuilder.withDescription("Number of pages to reserve.")
                                .hasArg()
                                .withArgName("NUMBER")
                                .create("numpages") );

        // set up the commandline parser
        CommandLineParser cliParser = new BasicParser();
        CommandLine cliCommand;
        boolean badCommand = true;

        //verify the commands passed in are valid, and determine appropriate mode
        try {
            cliCommand = cliParser.parse(cliOptions, args);
            if(cliCommand.hasOption("mode")) {
                String attemptedMode = cliCommand.getOptionValue("mode").trim();
                if(attemptedMode.equals("check")) {
                    mode = Mode.CHECK;
                    badCommand = false;
                } else if(attemptedMode.equals("reserve")) {
                    if(cliCommand.hasOption("procid") && cliCommand.hasOption("numpages")) {
                        mode = Mode.RESERVE;
                        procID = cliCommand.getOptionValue("procid").trim();
                        numPages = Integer.parseInt(cliCommand.getOptionValue("numpages").trim());
                        if(!procID.equals("") && numPages > 0) {
                            badCommand = false;
                        }
                    } else { System.out.println("Requirements not found..."); }
                } else if(attemptedMode.equals("run")) {
                    if(cliCommand.hasOption("procid")) {
                        mode = Mode.RUN;
                        procID = cliCommand.getOptionValue("procid").trim();
                        if(!procID.equals("")) {
                            badCommand = false;
                        }
                    }
                }
            }
        } catch (Exception e) {
            System.out.println("Error parsing command line: "+e.getMessage());
            //assume bad parameters passed, and print command line help
            HelpFormatter cliFormatter = new HelpFormatter();
            cliFormatter.printHelp("eMOP Controller", cliOptions);
            System.exit(-1);
        }

        //print help and exit if bad command line
        if(badCommand) {
            HelpFormatter cliFormatter = new HelpFormatter();
            cliFormatter.printHelp("eMOP Controller", cliOptions);
            System.exit(-1);
        }

        //run the emop controller given the appropriate mode
        try {
            EmopController emop = new EmopController();
            emop.init( mode, procID, numPages );

            if ( mode.equals(Mode.CHECK)) {
                emop.restartStalledJobs();
                emop.getPendingJobs();
            } else if(mode.equals(Mode.RESERVE)) {
                emop.reservePages();
            } else {
                emop.doWork();
            }
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

    /**
     * return the number of unreserved pages in the job queue
     * @throws SQLException
     */
    private void getPendingJobs() throws SQLException {
        int cnt = this.db.getJobCount();
        System.out.println(""+cnt);
    }

    /**
     * attempt to reserve pages, and return the number of pages reserved in the job queue given the procid
     * @throws SQLException
     */
    private void reservePages() throws SQLException {
        int reserved = this.db.tryReservingPages(procID, numPages);
        System.out.println(""+reserved);
    }

    /**
     * Initialize controller logging. In debug mode, just write all logs out to std:out
     * @param consoleLogger
     * @throws IOException
     */
    private void initLogging( ) throws IOException {
        LogManager.getRootLogger().removeAllAppenders();
        LogManager.getRootLogger().setLevel(Level.INFO);
        ConsoleAppender console = new ConsoleAppender(new PatternLayout("%d{E MMM dd, HH:mm:ss} [%p] - %m%n"));
        console.setThreshold(Level.INFO);
        console.activateOptions();
        LogManager.getRootLogger().addAppender(console);
    }

    /**
     * Initialize the controller. Get run settings from environment. Init database connection.
     * @throws SQLException
     * @throws OptionException
     * @throws FileNotFoundException
     */
    public void init( Mode mode, String ProcessID, int NumberPages ) throws IOException, SQLException {
        procID = ProcessID;
        numPages = NumberPages;

        if ( mode.equals(Mode.CHECK) || mode.equals(Mode.RESERVE)) {
            initWorkCheckMode();
            return;
        }

        // setup console logger. this will be re-routed when running
        // on brazos to a log file named with the job id
        initLogging() ;
        LOG.info("Initialize eMOP controller");

        // get required env settings
        getEnvironmentConfig();

        // Use them to read emop controller settings from local properties file
        Properties props = new Properties();
        FileInputStream fis = new FileInputStream(new File(this.emopHome,"emop.properties"));
        props.load(fis);

        // required DB stuff:
        initDatabase(props);

        // optional
        if ( props.containsKey("log_level")) {
            String level = props.getProperty("log_level");
            if ( level.equals("DEBUG")) {
                LOG.setLevel(Level.DEBUG);
            } else if ( level.equals("INFO")) {
                LOG.setLevel(Level.INFO);
            } else if ( level.equals("ERROR")) {
                LOG.setLevel(Level.ERROR);
            }
        }
        if ( props.containsKey("path_prefix")) {
            this.pathPrefix = props.getProperty("path_prefix");
        }
        if ( props.containsKey("jx_algorithm")) {
            String algo = props.getProperty("jx_algorithm").trim();
            this.algorithm = Algorithm.valueOf(algo.toUpperCase());
        }
        if ( props.containsKey("wall_time_sec")) {
            String timeStr = props.getProperty("wall_time_sec");
            this.wallTimeSec = Integer.parseInt(timeStr);
            this.timeLeftMs = this.wallTimeSec*1000;
        }
    }

    private void initWorkCheckMode() throws IOException, SQLException {
        this.emopHome =  new File(getClass().getProtectionDomain().getCodeSource().getLocation().getPath()).getParent();

        Properties props = new Properties();
        FileInputStream fis = new FileInputStream(new File(this.emopHome,"emop.properties"));
        props.load(fis);

        // required DB stuff:
        initDatabase(props);
    }

    private void getEnvironmentConfig() {
        this.emopHome =  new File(getClass().getProtectionDomain().getCodeSource().getLocation().getPath()).getParent();

        this.juxtaHome =  System.getenv("JUXTA_HOME");
        if ( this.juxtaHome == null || this.juxtaHome.length() == 0) {
            throw new RuntimeException("Missing required JUXTA_HOME environment variable");
        }

        this.retasHome =  System.getenv("RETAS_HOME");
        if ( this.retasHome == null || this.retasHome.length() == 0) {
            throw new RuntimeException("Missing required RETAS_HOME environment variable");
        }

        this.seasrHome = System.getenv("SEASR_HOME");
        if (this.seasrHome == null || this.seasrHome.length() == 0) {
            throw new RuntimeException("Missing required SEASR_HOME environment variable");
        }
    }

    private void initDatabase( Properties props ) throws IOException, SQLException {
        // pull settings .my.cf
        String dbHost = "localhost";
        if ( props.containsKey("db_host")) {
            dbHost = props.getProperty("db_host");
        }

        String dbName = props.getProperty("db_name");
        String dbUser = props.getProperty("db_user");
        String dbPass = props.getProperty("db_pass");

        this.db = new Database();
        this.db.connect(dbHost, dbName, dbUser, dbPass);
    }

    /**
     * Scan the job queue for jobs that have stayed in the PROCESSING state for too
     * long. Mark them as failed with a result of Timed Out.
     */
    public void restartStalledJobs() {
        // if something has been in process for longer 15 mins
        // the controller that started it has been killed and the job will
        // not complete. mark it as timed out
        try {
            this.db.restartStalledJobs( 15*60);
        } catch (SQLException e ) {
            LOG.error("Unable to restart long-running job", e);
        }
    }

    /**
     * Main work look for the controller. As long as time remains, pick off available jobs.
     * Mark the as in-process and kick off a task to service them. When complete,
     * record data to configured out location and mark task as pending post-processing.
     * @throws SQLException
     */
    public void doWork() throws SQLException {
        long totalMs = 0; //this keeps track of milliseconds spent in total
        long t0; //this keeps track of milliseconds spent on each page

        ArrayList<JobPage> jobs = this.db.getJobs(procID); //we get the jobs reserved for this procid
        if ( jobs.size() == 0 ) {
            LOG.info("No jobs to process. Terminating.");
        }

        for(int x = 0; x < jobs.size(); x++) { //iterate through the pages we have
            t0 = System.currentTimeMillis();
            // get details about the OCR batch that is to be
            // used for this jobs
            this.db.updateJobStatus(jobs.get(x).getId(), Status.PROCESSING);
            BatchJob batch = jobs.get(x).getBatch();
            LOG.info("Got job ["+jobs.get(x).getId()+"] - Batch: "+batch.getName()+" job Type: "+batch.getJobType()+", OCR engine: " + batch.getOcrEngine());
            if ( batch.getJobType().equals(JobType.GT_COMPARE)) {
                doGroundTruthCompare(jobs.get(x));
            } else {
                doOCR(jobs.get(x));
            }

            long durationMs = (System.currentTimeMillis()-t0);
            totalMs+= durationMs;
            LOG.info("Job ["+jobs.get(x).getId()+"] COMPLETE. Duration: "+(durationMs/1000f)+" secs");
        }
        LOG.info("==> TOTAL TIME: "+totalMs/1000f);
    }


    /**
     * Find the original scanned image for a page of a work. The retuned path will
     * include ant extra prefix to the DB path.
     *
     * @param workInfo
     * @param pageInfo
     * @return
     */
    private String getPageImage( WorkInfo workInfo, PageInfo pageInfo ) {
        // first, see if the image path was stored in DB. If so, use it
        String img = pageInfo.getPageImage();
        if ( img != null && img.trim().length() > 0 ) {
            return addPrefix(img);
        }

        // Not in database. Guess at path
        int pageNumber = pageInfo.getPageNumber();
        if ( workInfo.isEcco() ) {
            // ECCO format: ECCO number + 4 digit page + 0.tif
            img = String.format("%s/%s%04d0.TIF", workInfo.getEccoDirectory(), workInfo.getEccoNumber(), pageNumber );
            return addPrefix(img);
        } else {
            // EEBO format: 00014.000.001.tif where 00014 is the page number.
            // EEBO is a problem because of the last segment before .tif. It is some
            // kind of version info and can vary. Start with 0 and increase til
            // a file is found.
            int versionNum = 0;
            while (versionNum < 100) {
                img = String.format("%s/%05d.000.%03d.tif", workInfo.getEeboDirectory(), pageNumber, versionNum );
                img = addPrefix(img);
                LOG.debug("Looking for EEBO image: "+img);
                File test = new File(img);
                if ( test.exists() ) {
                    return img;
                }
                versionNum++;
            }
            return "";  // NOT FOUND!
        }
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
        String ocrXmlFile = workInfo.getOcrOutFile(job.getBatch(), OutputFormat.XML, pageInfo.getPageNumber());
        String ocrTxtFile = workInfo.getOcrOutFile(job.getBatch(), OutputFormat.TXT, pageInfo.getPageNumber());

        // try to determine location of original TIF image
        String pathToImage = getPageImage(workInfo, pageInfo);
        if ( pathToImage.length() == 0 ) {
            LOG.error("Job Failed - couldn't find page image");
            this.db.updateJobStatus(job.getId(), Status.FAILED, "Couldn't find page image");
            this.db.addPageResult(job, ocrTxtFile, ocrXmlFile, -1, -1);
            return;
        }

        // First, do the OCR
        try {
            // call the correct engine based upon batch config
            if ( job.getBatch().getOcrEngine().equals(OcrEngine.TESSERACT)) {
                LOG.debug("Using Tesseract to OCR "+pathToImage);
                doTesseractOcr( pathToImage, job.getBatch().getParameters(), addPrefix(ocrXmlFile), job.getTrainingFont() );
            } else {
                LOG.error("OCR with "+job.getBatch().getOcrEngine()+" not yet supported");
                this.db.updateJobStatus(job.getId(), Status.FAILED, job.getBatch().getOcrEngine()+" not supported");
                return;
            }
        } catch (InterruptedException e) {
            LOG.error("Job timed out");
            this.db.updateJobStatus(job.getId(), Status.FAILED, "OCR Timed Out");
            this.db.addPageResult(job, ocrTxtFile, ocrXmlFile, -1, -1);
            return;
        } catch ( Exception e ) {
            LOG.error("Job Failed", e);
            this.db.updateJobStatus(job.getId(), Status.FAILED, e.getMessage());
            this.db.addPageResult(job, ocrTxtFile, ocrXmlFile, -1, -1);
            return;
        }

        // TODO: I don't understand the workflow of the code... how updateJobStatus should work with what status
        // at what stage, so I don't feel comfortable messing with the code below. I've included a small code
        // snippet that shows how to retrieve the correctability scores as an example.
        // I've also added a method in Database.java called "addPostProcResult" that should probably be used
        // to record the various scores in the postproc_pages table.

        // Compute the page correctability score
        // float[] corrScores = computeCorrectabilityScore(ocrXmlFile);
        // float ecorr = corrScores[0];
        // float stats = corrScores[1];

        // If that was successful, see if GT compare is possible
        try {
            // Can we do GT compare on this page?
            if ( pageInfo.hasGroundTruth() == false ) {
                LOG.info("Ground truth does not exist for page "+job.getPageId());
                this.db.addPageResult(job, ocrTxtFile, ocrXmlFile, -1, -1);
                this.db.updateJobStatus(job.getId(), Status.PENDING_POSTPROCESS, "OCR Complete, no GT");
            } else {
                float juxtaVal = juxtaCompare(pageInfo.getGroundTruthFile(), ocrTxtFile);
                float retasVal = retasCompare(pageInfo.getGroundTruthFile(), ocrTxtFile);
                this.db.addPageResult(job, ocrTxtFile, ocrXmlFile, juxtaVal, retasVal);
                this.db.updateJobStatus(job.getId(), Status.PENDING_POSTPROCESS, "JuxtaCL: "+juxtaVal+", RETAS: "+retasVal);
            }

        } catch (InterruptedException e) {
            LOG.error("Job timed out");
            this.db.updateJobStatus(job.getId(), Status.FAILED, "OCR GT Compare Timed Out");
            this.db.addPageResult(job, ocrTxtFile, ocrXmlFile, -1, -1);
        } catch ( Exception e ) {
            LOG.error("Job Failed", e);
            this.db.updateJobStatus(job.getId(), Status.FAILED, e.getMessage());
            this.db.addPageResult(job, ocrTxtFile, ocrXmlFile, -1, -1);
        }
    }

    private float[] computeCorrectabilityScore(String ocrXmlFile) throws InterruptedException, IOException {
        LOG.debug("Computing the correctable score for: " + ocrXmlFile);

        ProcessBuilder pb = new ProcessBuilder(
                "java", "-Xms128M", "-Xmx128M", "-jar",
                this.seasrHome+"/PageEvaluator.jar",
                "-q", addPrefix( ocrXmlFile )
        );

        Process proc = null;
        try {
            proc = pb.start();
            awaitProcess(proc, JX_TIMEOUT_MS);

            if (proc.exitValue() == 0) {
                String out = IOUtils.toString(proc.getInputStream()).trim();
                String[] scores = out.split(",");
                if (scores.length != 2)
                    throw new IOException("Unexpected response format: " + out);

                return new float[] { Float.parseFloat(scores[0]), Float.parseFloat(scores[1]) };
            } else {
                String err = IOUtils.toString(proc.getErrorStream());
                throw new IOException(err);
            }
        }
        finally {
            if (proc != null)
                proc.destroy();
        }
    }

    private float juxtaCompare(String gtFile, String ocrTxtFile) throws InterruptedException, IOException, SQLException {
        LOG.debug("Compare OCR results with ground truth using JuxtaCL");

        String out = "";
        String cmd = this.juxtaHome+"/juxta-cl.jar";
        String gt = addPrefix( gtFile );
        String ocr = addPrefix(ocrTxtFile);
        String alg = this.algorithm.toString().toLowerCase();

        ProcessBuilder pb = new ProcessBuilder(
            "java", "-Xms128M", "-Xmx128M", "-jar",
            cmd, "-diff", gt, ocr,
            "-algorithm", alg, "-hyphen", "none");
        pb.directory( new File(this.juxtaHome) );
        Process jxProc = pb.start();
        awaitProcess(jxProc, JX_TIMEOUT_MS);
        out = IOUtils.toString(jxProc.getInputStream());

        if (jxProc.exitValue() == 0) {
            jxProc.destroy();
            return Float.parseFloat(out.trim());
        } else {
            LOG.error(out);
            jxProc.destroy();
            throw new IOException( out);
        }
    }

    private float retasCompare(String gtFile, String ocrTxtFile) throws InterruptedException, IOException, SQLException {
        LOG.debug("Compare OCR results with ground truth using RETAS");
        String out = "";
        ProcessBuilder pb = new ProcessBuilder(
            "java",  "-Xms128M", "-Xmx128M", "-jar",
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
            jxProc.destroy();
            return Float.parseFloat(out.trim().split("\t")[2]);
        } else {
            LOG.error(out);
            jxProc.destroy();
            throw new IOException( out);
        }
    }

    /**
     * Use Tesseract to OCR the specified image file. Pass along any params from the batch
     *
     * @param img Path to the page image
     * @param priorVersionCnt number of pre-existing ocr'd versions of this page
     * @param params (may be null) Parameters to pass along to tesseract
     * @param trainingFont
     * @return Name of the OCR text file
     * @throws InterruptedException
     * @throws IOException
     * @throws TransformerException
     * @throws SAXException
     */
    private void doTesseractOcr(String pageImage, String params, String outFile, String trainingFont) throws InterruptedException, IOException, SAXException, TransformerException {
        // ensure that the directory tree is present
        File out = new File(outFile);
        out.getParentFile().mkdirs();
        String exe = "tesseract";

        // NOTE: strip the .txt extension; tesseract auto-appends it
        final String trimmedOut = outFile.substring(0,outFile.length()-4);

        // kickoff the OCR engine and wait until it completes
        File cfgFile = new File(this.emopHome, "tess_cfg.txt" );
        ProcessBuilder pb = new ProcessBuilder( exe, pageImage, trimmedOut, "-l", trainingFont, cfgFile.getAbsolutePath() );
        LOG.info("Command: "+pb.command());
        Process jxProc = pb.start();
        awaitProcess(jxProc, JX_TIMEOUT_MS);
        if (jxProc.exitValue() != 0) {
            String err = IOUtils.toString(jxProc.getErrorStream());
            jxProc.destroy();
            throw new RuntimeException("OCR failed: "+err);
        }
        jxProc.destroy();

        // NOTES -
        // MJC: the latest version of Tesseract was installed by Trey (10/10/13)
        //      and it now seems to produce both hOCR, with .hocr extension

        LOG.debug("Renaming "+trimmedOut+".hocr to "+trimmedOut+".xml");
        Runtime.getRuntime().exec("mv "+trimmedOut+".hocr "+trimmedOut+".xml" );
    }

    private void doGroundTruthCompare(JobPage job) throws SQLException {
        // get details about the page associated with this job
        PageInfo pageInfo = this.db.getPageInfo(job.getPageId());
        if ( pageInfo.hasGroundTruth() == false) {
            LOG.info("Ground truth does not exist for page "+job.getPageId());
            this.db.updateJobStatus(job.getId(), Status.PENDING_POSTPROCESS, "GT does not exist");
            return;
        }

        // GT exists, grab the path
        String gtPath = pageInfo.getGroundTruthFile();

        // now pull page result based on OCR engine
        LOG.debug("pageID "+job.getPageId()+", engineID: "+job.getBatch().getOcrEngine());
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
        if ( this.pathPrefix != null && this.pathPrefix.trim().length() > 0 ) {
            File file1 = new File(this.pathPrefix);
            File file2 = new File(file1, path);
            return file2.getPath();
        }
        return path;
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
        if ( this.db != null ) {
            this.db.disconnect();
        }
    }
}

package mil.nga.bundler.ejb;

import java.util.List;
import java.util.Calendar;

import javax.ejb.EJB;
import javax.ejb.LocalBean;
import javax.ejb.Stateless;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import mil.nga.bundler.model.BundlerJobMetrics;
import mil.nga.bundler.ejb.exceptions.EJBLookupException;
import mil.nga.bundler.ejb.jdbc.JDBCJobService;
import mil.nga.bundler.interfaces.BundlerConstantsI;
import mil.nga.bundler.ejb.jdbc.JDBCArchiveService;
import mil.nga.bundler.ejb.jdbc.JDBCFileService;
import mil.nga.bundler.ejb.jdbc.JDBCJobMetricsService;

/**
 * Session Bean implementation class DatasourceCleanupService
 * 
 * This session bean was implemented in order to manage the number of job 
 * records maintained in target datasources utilized by the bundler 
 * application.
 */
@Stateless
@LocalBean
public class DatasourceCleanupService 
        implements BundlerConstantsI {

    /**
     * Set up the Log4j system for use throughout the class
     */        
    private static final Logger LOGGER = 
            LoggerFactory.getLogger(DatasourceCleanupService.class);
    
    /**
     * The number of days in the past after which records should be removed 
     * from the database.
     */
    public static final int DELETE_JOBS_OLDER_THAN = 14;
    
    /**
     * Handle to the interface associated with the ARCHIVE_JOBS table.
     */
    @EJB
    JDBCArchiveService archiveService;
    
    /**
     * Handle to the interface associated with the FILE_ENTRY table.
     */
    @EJB
    JDBCFileService fileService;
    
    /**
     * Handle to the interface associated with the JOBS table.
     */
    @EJB
    JDBCJobService jobService;
    
    /**
     * Handle to the interface associated with the BUNDLER_JOB_METRICS table.
     */
    @EJB
    JDBCJobMetricsService jobMetricsService;
    
    /**
     * Default no-arg constructor. 
     */
    public DatasourceCleanupService() { }
    
    /**
     * Private method used to obtain a reference to the target EJB.  
     * @return Reference to the JDBCArchiveService EJB.
     */
    private JDBCArchiveService getJDBCArchiveService() 
            throws EJBLookupException {
        if (archiveService == null) {
            LOGGER.warn("Application container failed to inject the "
                    + "reference to the JDBCJobService EJB.  Attempting "
                    + "to look it up via JNDI.");
            archiveService = EJBClientUtilities
                    .getInstance()
                    .getJDBCArchiveService();
        }
        return archiveService;
    }
    
    
    /**
     * Private method used to obtain a reference to the target EJB.  
     * @return Reference to the JDBCArchiveService EJB.
     */
    private JDBCFileService getJDBCFileService() 
            throws EJBLookupException {
        if (fileService == null) {
            LOGGER.warn("Application container failed to inject the "
                    + "reference to the JDBCJobService EJB.  Attempting "
                    + "to look it up via JNDI.");
            fileService = EJBClientUtilities
                    .getInstance()
                    .getJDBCFileService();
        }
        return fileService;
    }
    
    /**
     * Private method used to obtain a reference to the target EJB.  
     * @return Reference to the JDBCJobMetricsService EJB.
     */
    private JDBCJobMetricsService getJDBCJobMetricsService() 
            throws EJBLookupException {
        if (jobMetricsService == null) {
            LOGGER.warn("Application container failed to inject the "
                    + "reference to the JDBCJobMetricsService EJB.  Attempting "
                    + "to look it up via JNDI.");
            jobMetricsService = EJBClientUtilities
                    .getInstance()
                    .getJDBCJobMetricsService();
        }
        return jobMetricsService;
    }
    
    /**
     * Private method used to obtain a reference to the target EJB.  
     * @return Reference to the JDBCJobService EJB.
     */
    private JDBCJobService getJDBCJobService() 
            throws EJBLookupException {
        if (jobService == null) {
            LOGGER.warn("Application container failed to inject the "
                    + "reference to the JDBCJobService EJB.  Attempting "
                    + "to look it up via JNDI.");
            jobService = EJBClientUtilities
                    .getInstance()
                    .getJDBCJobService();
        }
        return jobService;
    }
    
    /**
     * Method introduced to ensure that the record to delete has a metrics
     * record associated with it prior to removing from the data source.
     * 
     * @param jobID The job ID to check.
     * @return True if the input Job ID has an associated metrics record,
     * False otherwise. 
     */
    private boolean hasMetrics(String jobID) {
        boolean hasMetrics = false;
        try {
            if (getJDBCJobMetricsService().getJobMetrics(jobID) != null) {
                hasMetrics = true;
            }
        }
        catch (EJBLookupException ele) {
            LOGGER.error("Unexpected EJBLookupException raised while "
                    + "attempting to look up [ "
                    + ele.getEJBName()
                    + " ].  Exception message [ "
                    + ele.getMessage()
                    + " ]");
        }
        return hasMetrics;
    }
    
    /**
     * Calculate the time exactly 2 weeks ago.
     * @return The time exactly 2 weeks ago.
     */
    private long getPurgeTime() {
        Calendar cal = Calendar.getInstance();  
        cal.add(Calendar.DAY_OF_YEAR, -DELETE_JOBS_OLDER_THAN);  
        return cal.getTimeInMillis(); 
    }
    
    /**
     * Method implemented to search for and delete any "orphaned" records left
     * in the FILE_ENTRY table.
     */
    public void cleanOrphanedFileRecords() {
        try {
            List<String> allJobIDs = getJDBCJobService().getJobIDs();
            List<String> fileJobIDs = getJDBCFileService().getJobIDs();
            if (fileJobIDs != null) {
                fileJobIDs.removeAll(allJobIDs);
                if ((fileJobIDs != null) && (fileJobIDs.size() > 0)) {
                    for (String jobID : fileJobIDs) {
                        if (LOGGER.isDebugEnabled()) {
                            LOGGER.debug("Found orphaned FILE_ENTRY records "
                                    + "for job [ "
                                    + jobID
                                    + " ].  Removing...");
                        }
                        getJDBCFileService().deleteFiles(jobID);
                    }
                }
            }
        }
        catch (EJBLookupException ele) {
            LOGGER.error("Unexpected EJBLookupException raised while "
                    + "attempting to look up [ "
                    + ele.getEJBName()
                    + " ].  Exception message [ "
                    + ele.getMessage()
                    + " ].  Any potential orphaned file records will not be "
                    + "cleaned up.");
        }
    }
    
    /**
     * Method implemented to search for and delete any "orphaned" records left
     * in the ARCHIVE_JOBS table.
     */
    public void cleanOrphanedArchiveRecords() {
        try {
            List<String> allJobIDs = getJDBCJobService().getJobIDs();
            List<String> archiveJobIDs = getJDBCArchiveService().getJobIDs();
            if (archiveJobIDs != null) {
                archiveJobIDs.removeAll(allJobIDs);
                if ((archiveJobIDs != null) && (archiveJobIDs.size() > 0)) {
                    for (String jobID : archiveJobIDs) {
                        if (LOGGER.isDebugEnabled()) {
                            LOGGER.debug("Found orphaned ARCHIVE_JOBS records "
                                    + "for job [ "
                                    + jobID
                                    + " ].  Removing...");
                        }
                        getJDBCArchiveService().deepDeleteArchive(jobID);
                    }
                }
            }
        }
        catch (EJBLookupException ele) {
            LOGGER.error("Unexpected EJBLookupException raised while "
                    + "attempting to look up [ "
                    + ele.getEJBName()
                    + " ].  Exception message [ "
                    + ele.getMessage()
                    + " ].  Any potential orphaned archive records will not be "
                    + "cleaned up.");
        }
    }
    
    /**
     * Delete all jobs from the target datasource that are older than the 
     * pre-defined time limit.  Currently two weeks.
     */
    public void purgeOldJobRecords() {
        
        long purgeTime = getPurgeTime();
        
        try {
            List<String> oldJobs = getJDBCJobService().getJobIDs(purgeTime);
            if ((oldJobs != null) && (oldJobs.size() > 0)) { 
                for (String jobID : oldJobs) {
                    if (hasMetrics(jobID)) {
                        if (LOGGER.isDebugEnabled()) {
                            LOGGER.info("Removing old job [ "
                                    + jobID 
                                    + " ].");
                        }
                        getJDBCJobService().delete(jobID);
                    }
                    else {
                       LOGGER.warn("Job ID [ " + jobID + " ] is more than two "
                               + "weeks old, yet it does not have an associated "
                               + "metrics object.  Please investigate.");
                    }
                }
            }
            else {
                if (LOGGER.isDebugEnabled()) { 
                    LOGGER.debug("There are currently no old jobs to "
                            + "process.");
                }
            }
        }
        catch (EJBLookupException ele) {
            LOGGER.error("Unexpected EJBLookupException raised while "
                    + "attempting to look up [ "
                    + ele.getEJBName()
                    + " ].  Exception message [ "
                    + ele.getMessage()
                    + " ].  Old Job records will not be cleaned up.");
        }
    }
    
    /**
     * Publicly exposed method called to invoke the datasource cleanup 
     * operation.
     */
    public void cleanup() {
        
        long startTime = System.currentTimeMillis();
        
        purgeOldJobRecords();
        cleanOrphanedArchiveRecords();
        cleanOrphanedFileRecords();
        
        LOGGER.info("Datasource cleanup completed in [ "
                + (System.currentTimeMillis() - startTime)
                + " ] ms.");
    }

    
}

package mil.nga.bundler.ejb;

import javax.ejb.EJB;
import javax.ejb.Schedule;
import javax.ejb.Singleton;
import javax.ejb.Timer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import mil.nga.bundler.EJBClientUtilities;
import mil.nga.bundler.interfaces.BundlerConstantsI;
import mil.nga.util.FileUtils;

/**
 * Timer Bean implemented to cleanup the staging area directory used by both 
 * the the bundler and the PDF merge utility.  This timer will fire every day 
 * a 12:30 a.m. and delete all staging directories older than 48 hours.
 * 
 * @author L. Craig Carpenter
 */
@Singleton
public class CleanupTimerBean 
        implements BundlerConstantsI {

    /**
     * Set up the Log4j system for use throughout the class
     */        
    private static Logger LOGGER = LoggerFactory.getLogger(
            CleanupTimerBean.class);
    
    /**
     * Container-injected reference to the CleanupService object.
     */
    @EJB
    DiskCleanupService cleanupService;
    
    /**
     * Default no-arg constructor. 
     */
    public CleanupTimerBean() {  }
    
    /**
     * Private method used to obtain a reference to the target EJB.  
     * @return Reference to the JobService EJB.
     */
    private DiskCleanupService getDiskCleanupService() {
        if (cleanupService == null) {
            LOGGER.warn("Application container failed to inject the "
                    + "reference to [ CleanupService ].  Attempting to "
                    + "look it up via JNDI.");
            cleanupService = EJBClientUtilities
                    .getInstance()
                    .getCleanupService();
        }
        return cleanupService;
    }
    
    /**
     * Entry point called by the application container to run the staging 
     * area cleanup algorithm.
     * 
     * @param t Container injected Timer object.
     */
    @Schedule(second="0", minute="30", hour="0", dayOfWeek="*",
              dayOfMonth="*", month="*", year="*", info="CleanupTimer")
    private void scheduledTimeout(final Timer t) {

        LOGGER.info("CleanupTimerBean launched at [ "
                + FileUtils.getTimeAsString(
                        UNIVERSAL_DATE_STRING, 
                        System.currentTimeMillis())
                + " ].");
        
        if (getDiskCleanupService() != null) {
            getDiskCleanupService().cleanup();
        }

        LOGGER.info("CleanupTimerBean complete at [ "
                + FileUtils.getTimeAsString(
                        UNIVERSAL_DATE_STRING, 
                        System.currentTimeMillis())
                + " ].");
    }
}

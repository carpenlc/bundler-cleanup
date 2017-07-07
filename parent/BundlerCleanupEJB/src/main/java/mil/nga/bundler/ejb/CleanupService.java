package mil.nga.bundler.ejb;

import java.io.File;
import java.io.IOException;
import java.util.Calendar;
import java.util.List;

import javax.annotation.PostConstruct;
import javax.ejb.LocalBean;
import javax.ejb.Stateless;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import mil.nga.bundler.FileNameGenerator;
import mil.nga.bundler.interfaces.BundlerConstantsI;
import mil.nga.util.CaseInsensitiveDirFilter;
import mil.nga.util.FileUtils;

/**
 * Session Bean implementation class CleanupService
 */
@Stateless
@LocalBean
public class CleanupService implements BundlerConstantsI {

    /**
     * Set up the Log4j system for use throughout the class
     */        
    Logger LOGGER = LoggerFactory.getLogger(CleanupService.class);
    
    /**
     * The location of the staging area.
     */
    private File stagingDirectory = null;
    
    /**
     * Initialization method used to populate the private internal 
     * stagingDirectory variable.
     */
    @PostConstruct
    public void init() {
        
        String dir    = FileNameGenerator.getInstance().getStagingDirectory();
        
        if ((dir != null) && (!dir.isEmpty())) {
            stagingDirectory = new File(dir);
            if (stagingDirectory.exists()) {
                if (!stagingDirectory.isDirectory()) {
                    
                    LOGGER.error("The location identified by system property [ "
                            + STAGING_DIRECTORY_PROPERTY
                            + " ] value [ "
                            + dir 
                            + " ] defines a location that is not a directory.  "
                            + "The cleanup service will not run.");
                    stagingDirectory = null;
                    
                }
            }
            else {
                
                LOGGER.error("The location identified by system property [ "
                        + STAGING_DIRECTORY_PROPERTY
                        + " ] value [ "
                        + dir 
                        + "] does not exist.  "
                        + "The cleanup bean will not run.");
                stagingDirectory = null;
            }
        }
        else {
            LOGGER.error("Unable to determine the location of the bundler "
                    + "staging directory.  Please check the value of "
                    + "property ["
                    + STAGING_DIRECTORY_PROPERTY
                    + "].  The cleanup bean will not run.");
        }
    }
    
    /**
     * Default constructor. 
     */
    public CleanupService() { }

    /**
     * Calculate the time 48 hours ago.
     * @return The time 48 hours ago.
     */
    private long getPurgeTime() {
         Calendar cal = Calendar.getInstance();  
         cal.add(Calendar.DAY_OF_MONTH, -2);  
         return cal.getTimeInMillis(); 
    }
    
    /**
     * Determine whether or not the input file should be deleted.
     * This method ensures the input file is a directory and falls 
     * outside the date threshold.
     * 
     * @param file Candidate for deletion.
     * @return True if the file should be deleted.  False otherwise.
     */
    private boolean delete(File file) {
        boolean delete    = false;
        long    purgeTime = getPurgeTime();
        if ((file != null) && (file.exists()) && (file.isDirectory())) {
            if (file.lastModified() < purgeTime) {
                delete = true;
            }
        }
        return delete;
    }
    
    public void cleanup() {
        
        List<String> regexes = FileNameGenerator.getRegEx();
        
        if (stagingDirectory != null) {
            for (String regex : regexes) {
                
                if (LOGGER.isDebugEnabled()) {
                    LOGGER.debug("Checking for on-disk bundles to remove.  "
                            + "Using staging directory ["
                            + stagingDirectory.getAbsolutePath() 
                            + "] and regex ["
                            + regex
                            + "].");
                }
                
                File[] dirs  = stagingDirectory.listFiles(
                                    new CaseInsensitiveDirFilter(regex));
    
                if ((dirs != null) && (dirs.length > 0)) {
                    
                    for (File file : dirs) {
                        
                        if (LOGGER.isDebugEnabled()) {
                            LOGGER.debug("Checking directory [ "
                                    + file.getAbsolutePath()
                                    + " ].");
                        }
                        
                        if (delete(file)) {
                            try {
                                LOGGER.info("Deleting expired directory [ "
                                        + file.getAbsolutePath()
                                        + " ].");
                                FileUtils.delete(file);
                            }
                            catch (IOException ioe) {
                                LOGGER.warn("An IOException was encountered "
                                        + "while attempting to delete file [ "
                                        + file.getAbsolutePath()
                                        + " ].  Error encountered [ "
                                        + ioe.getMessage()
                                        + " ].");
                            }
                        }
                    }
                }
                else {
                    LOGGER.info("There are no files in the staging "
                            + "directory.");
                }
            } // end loop: regex
        }
        else {
            LOGGER.error("Unable to determine the target staging directory.  "
                    + "The cleanup service will not run.");
        }
    }
}

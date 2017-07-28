package mil.nga.bundler.ejb;

import java.io.IOException;
import java.net.URI;
import java.nio.file.DirectoryStream;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import javax.annotation.PostConstruct;
import javax.ejb.LocalBean;
import javax.ejb.Stateless;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import mil.nga.PropertyLoader;
import mil.nga.bundler.exceptions.PropertiesNotLoadedException;
import mil.nga.bundler.interfaces.BundlerConstantsI;

/**
 * Session Bean implementation class CleanupService
 */
@Stateless
@LocalBean
public class DiskCleanupService 
        extends PropertyLoader 
        implements BundlerConstantsI {

    /**
     * Set up the Log4j system for use throughout the class
     */        
    private static final Logger LOGGER = 
            LoggerFactory.getLogger(DiskCleanupService.class);
    
    /**
     * The number of days in the past after which directories should 
     * be removed from the database.
     */
    public static final int DELETE_DATA_OLDER_THAN = 14;
    
    /**
     * Default no-arg constructor. 
     */
    public DiskCleanupService() {
        super(PROPERTY_FILE_NAME);
    }
    
    /**
     * The location of the staging area.
     */
    private URI stagingDirectory = null;
    
    /**
     * Initialization method used to populate the private internal 
     * stagingDirectory variable.
     */
    @PostConstruct
    public void init() {
        setStagingDirectory();
    }
    
    /**
     * Calculate the time two weeks ago.
     * @return The time two weeks ago.
     */
    private long getPurgeTime() {
         Calendar cal = Calendar.getInstance();  
         cal.add(Calendar.DAY_OF_MONTH, -DELETE_DATA_OLDER_THAN);  
         return cal.getTimeInMillis(); 
    }
    
    /**
     * Delete a single file.
     * 
     * @param p Path object defining a single file.
     */
    private void delete(Path p) {
        if ((p != null) && (Files.exists(p))) {
            try {
                Files.delete(p);
            }
            catch (IOException ioe) {
                LOGGER.warn("Unexpected IOException while removing target "
                        + "directory [ "
                        + p.toUri().toString()
                        + " ].  Exception message => [ "
                        + ioe.getMessage()
                        + " ].  Target file not deleted.");
            }
        }
    }
    
    /**
     * Implementation of the NIO2 recursive delete operation.
     * 
     * @param p Directory to delete.
     */
    private void deleteDir (Path p) {
        if (p != null) {
            try {
    
                LOGGER.info("Removing expired directory [ "
                        + p.toUri().toString()
                        + " ].");
                
                Files.walkFileTree(p, new SimpleFileVisitor<Path>() {
                    @Override
                    public FileVisitResult visitFile(
                            Path file, 
                            BasicFileAttributes attrs) throws IOException {
                        Files.delete(file);
                        return FileVisitResult.CONTINUE;
                    }
                    @Override
                    public FileVisitResult postVisitDirectory(
                            Path dir, 
                            IOException ioe) throws IOException {
                        Files.delete(dir);
                        return FileVisitResult.CONTINUE;
                    }
                });
                
            }
            catch (IOException ioe) {
                LOGGER.warn("Unexpected exception while removing target "
                        + "directory [ "
                        + p.toUri().toString()
                        + " ].  Exception message => [ "
                        + ioe.getMessage()
                        + " ].  Target file not deleted.");
            }
        }
        else {
            LOGGER.warn("Input Path object is null.  Nothing to delete.");
        }
    }
    
    /**
     * Determine whether or not the input file should be deleted.
     * This method ensures the input file is a directory and falls 
     * outside the date threshold.
     * 
     * @param file Candidate for deletion.
     * @return True if the file should be deleted.  False otherwise.
     */
    private boolean timeToDelete(Path p) {
        
        boolean delete    = false;
        long    purgeTime = getPurgeTime();
        
        if (p != null) {
            try {
                
                BasicFileAttributes attrs = 
                        Files.readAttributes(p, BasicFileAttributes.class);
                
                if (attrs != null) {
                    FileTime t = attrs.creationTime();
                    if (t.toMillis() < purgeTime) {
                        delete = true;
                    }
                }
                else {
                    LOGGER.warn("Unable to read the file attributes "
                            + "associated with file [ "
                            + p.toUri().toString()
                            + " ].  File will not be deleted.");
                }
            }
            catch (IOException ioe) {
                LOGGER.error("Unexpected IOException attempting to obtain "
                        + "the creation time associated with URI [ "
                        + p.toUri().toString()
                        + " ].  Exception message => [ "
                        + ioe.getMessage()
                        + " ].  Target file not deleted.");
            }
        }
        return delete;
    }
    
    /**
     * Method updated to utilize the NIO2 libraries to obtain a directory
     * listing.
     * 
     * @return A list of Path objects that are sub-directories of the target 
     * staging area.
     */
    public List<Path> getDirectoryListing() {
        
        List<Path> listing = new ArrayList<Path>();
        
        if (getStagingDirectory() != null) {
            if (Files.exists(Paths.get(getStagingDirectory()))) {
                try (DirectoryStream<Path> directoryStream = 
                        Files.newDirectoryStream(
                                Paths.get(getStagingDirectory()))) {
                    for (Path path : directoryStream) {
                        listing.add(path);
                    }
                }
                catch (IOException ioe) {
                    LOGGER.warn("An unexpected IOException was encountered "
                            + "while attempting to obtain a list of "
                            + "directory [ "
                            + getStagingDirectory().toString()
                            + " ].  Exception message [ "
                            + ioe.getMessage()
                            + " ].");
                }
            }
            else {
                LOGGER.error("The target staging area defined by URI [ "
                        + getStagingDirectory().toString()
                        + " ] does not exist.");
            }
        }
        else {
            LOGGER.error("Target staging area not defined.  The disk cleanup "
                    + "process will not execute.");
        }
        return listing;
    }
    
    /**
     * Public entry point for the session bean.  This method contains all of 
     * the logic required to clean up the on-disk staging area utilized by 
     * the Bundler application.  
     */
    public void cleanup() {
        
        int        count     = 0;
        List<Path> listing   = getDirectoryListing();
        long      startTime = System.currentTimeMillis();
        
        if ((listing != null) && (listing.size() > 0)) { 
            for (Path p : listing) {
                if (timeToDelete(p)) {
                    count++;
                    LOGGER.info("Recursively deleting directory [ "
                            + p.toUri().toString()
                            + " ].");
                    deleteDir(p);
                }
                else {
                    if (LOGGER.isDebugEnabled()) {
                        LOGGER.debug("File [ "
                                + p.toUri().toString()
                                + " ] not ready to delete.");
                    }
                }
            }
        }
        LOGGER.info("Staging area cleanup completed in [ "
                + (System.currentTimeMillis() - startTime)
                + " ] ms and removed [ "
                + count
                + " ] directories.");
    }
    
    /**
     * Getter method for the location of the temporary staging directory.
     * @param value The URI for the temporary staging directory.
     */
    public URI getStagingDirectory() {
        return stagingDirectory;
    }
    
    /**
     * Setter method for the location of the temporary staging directory.
     * @param value The temporary staging directory.
     */
    private void setStagingDirectory() 
            throws IllegalArgumentException {
        
        String value = null;
        
        try {
            value = getProperty(STAGING_DIRECTORY_PROPERTY);
            if ((value == null) || (value.isEmpty())) {
                LOGGER.error("Value for staging directory is null or empty.  "
                        + "Please check the value of property [ "
                        + STAGING_DIRECTORY_PROPERTY
                        + " ].");
            }
            else {
                stagingDirectory = URI.create(value);
            }
        }
        catch (IllegalArgumentException iae) {
            LOGGER.error("Unexpected IllegalArgumentException raised while "
                    + "attempting to convert the staging area location to "
                    + " a URI.  Staging area location [ "
                    + value
                    + " ].  Exception message => [ "
                    + iae.getMessage()
                    + " ].");
            stagingDirectory = null;
        }
        catch (PropertiesNotLoadedException pnle) {
            LOGGER.error("Unexpected PropertiesNotLoadedException raised while "
                    + "attempting to obtain the system properties.  Please "
                    + "check for the existance of property file [ "
                    + PROPERTY_FILE_NAME
                    + " ].");
            stagingDirectory = null;
        }
    }
}

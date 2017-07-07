package mil.nga.bundler;

import javax.ejb.EJB;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import mil.nga.bundler.ejb.CleanupService;
import mil.nga.bundler.interfaces.BundlerConstantsI;
import mil.nga.util.FileUtils;
import mil.nga.util.HostNameUtils;

/**
 * Simple JAX-RS endpoint providing an "isAlive" function for monitoring
 * purposes.  It also provides a method to use to manually start the cleanup
 * process.
 * 
 * @author L. Craig Carpenter
 */
@Path("")
public class BundlerCleanup implements BundlerConstantsI {

    /**
     * Set up the Log4j system for use throughout the class
     */
    static final Logger LOGGER = LoggerFactory.getLogger(BundlerCleanup.class);
    
    /**
     * The name of the application
     */
    public static final String APPLICATION_NAME = "BundlerCleanup";
    
    /**
     * Container-injected EJB reference.
     */
    @EJB
    CleanupService cleanupService;
    
    /**
     * Private method used to obtain a reference to the target EJB.  
     * @return Reference to the JobService EJB.
     */
    private CleanupService getCleanupService() {
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
     * Simple method used to determine whether or not the 
     * application is responding to requests.
     */
    @GET
    @Path("/isAlive")
    public Response isAlive(@Context HttpHeaders headers) {
        
        StringBuilder sb = new StringBuilder();
        sb.append("Application [ ");
        sb.append(APPLICATION_NAME);
        sb.append(" ] on host [ ");
        sb.append(HostNameUtils.getHostName());
        sb.append(" ] running in JVM [ ");
        sb.append(EJBClientUtilities.getInstance().getServerName());
        sb.append(" ].");
        
        return Response.status(Status.OK).entity(sb.toString()).build();
            
    }
    
    /**
     * REST endpoint allowing the cleanup process to be invoked via a web call.
     * @return Simple string notifying when the cleanup process is completed.
     */
    @GET
    @Path("/startCleanup")
    public String startCleanup() {
        
        LOGGER.info("CleanupService manually launched at [ "
                + FileUtils.getTimeAsString(
                        UNIVERSAL_DATE_STRING, 
                        System.currentTimeMillis())
                + " ].");
        
        if (getCleanupService() != null) {
            getCleanupService().cleanup();
        }
        else {
            return "Unable to look up timer service!";
        }

        LOGGER.info("CleanupService manual launch complete at [ "
                + FileUtils.getTimeAsString(
                        UNIVERSAL_DATE_STRING, 
                        System.currentTimeMillis())
                + " ].");
        
        return "Done!";
    }
    
}


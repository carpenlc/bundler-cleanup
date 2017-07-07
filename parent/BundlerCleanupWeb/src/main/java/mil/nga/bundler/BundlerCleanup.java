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

import mil.nga.bundler.ejb.CleanupTimerBean;
import mil.nga.util.HostNameUtils;

/**
 * Simple JAX-RS endpoint providing an "isAlive" function for monitoring
 * purposes.  It also provides a method to use to manually start the cleanup
 * process.
 * 
 * @author L. Craig Carpenter
 */
@Path("")
public class BundlerCleanup {

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
    CleanupTimerBean timerBean;
    
    /**
     * Private method used to obtain a reference to the target EJB.  
     * 
     * @return Reference to the CleanupTimerBean EJB.
     */
    private CleanupTimerBean getTimerBean() {
        if (timerBean == null) {
            
            LOGGER.warn("Application container failed to inject the "
                    + "reference to CleanupTimerBean.  Attempting to "
                    + "look it up via JNDI.");
            timerBean = EJBClientUtilities
                    .getInstance()
                    .getCleanupTimerBean();
        }
        return timerBean;
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
    
    @GET
    @Path("/startCleanup")
    public String startCleanup() {
        
            if (getTimerBean() != null) {
                
                getTimerBean().scheduledTimeout(null);
                
            }
            else {
                return "Unable to look up timer service!";
            }
        return "Done!";
        
    }
    
}


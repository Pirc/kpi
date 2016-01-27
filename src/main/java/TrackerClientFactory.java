package pirc.kpi;

/**
 * Factory interface responsible for providing TrackerClient instances
 * to the application.
 */
public interface TrackerClientFactory {
   /**
    * The locate method tells creates a new TrackerClient instance
    * at the specified path.  The client is 
    * automatically bound to the tracker tree and ready for use 
    * immediately.
    *
    * @param path Path for the tracker to be placed in the tree.
    */
    TrackerClient locate(String path);
}

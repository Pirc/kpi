package pirc.kpi;

/**
 * The LogTrackerClient inteface provides the API for sending a 
 * messages to a LogTracker.
 */
public interface LogTrackerClient extends TrackerClient {
   /**
    * @param msg Log message to be sent (at the info level) to the LogTracker.
    */
    void info(String msg);

   /**
    * @param msg Log message to be sent (at the warning level) to 
    * the LogTracker.
    */
    void warning(String msg);

   /**
    * @param msg Log message to be sent (at the error level) to 
    * the LogTracker.
    */
    void error(String msg);
}

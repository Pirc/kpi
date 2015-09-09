package pirc.kpi;

/**
 * The CounterTrackerClient inteface provides the API for sending a 
 * "bump" to a CounterTracker.
 */
public interface CounterTrackerClient extends TrackerClient {
   /** 
    * @param amt Integer containing the (positive or negative) amount to bump
    *   the counter by.
    */
    void bump(Integer amt);
}

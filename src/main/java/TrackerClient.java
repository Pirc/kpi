package pirc.kpi;

/**
 * TrackerClients are applications that bind to trackers in the TrackerTree
 * for the purpose of reporting some sort of status change or event.  Clients
 * "bind" to Trackers, which has the effect of either finding an existing
 * tracker at the specified path or of creating a new one.
 */
public interface TrackerClient {
   /**
    * TrackerClients are able to detach from the tree and clean up their
    * own internal actor but not have an effect on the Tracker that is in 
    * the tree.  This would be done in applications that want to connect and
    * dump a message into a Tracker but don't persist beyond the scope
    * of a single call.  For example, if we are tracking the calls that are
    * being made in a single user's web session, we would could bind to the 
    * tracker tree in the login service to create the tracker but then 
    * detach at the end of that service to make sure that our client is all
    * cleaned up but that the tracker is still present over in the tree to 
    * receive events from other service calls over the life of the session.
    */
    void detach();

   /** 
    * This method should be called when a TrackerClient would like to not 
    * only shut itself down but also to remove from the tree that Tracker
    * that it had initially bound to.  This makes sense for situations 
    * where the Tracker client is an object that lives for a certain amount
    * of time and wants to makes its status available in the TrackerTree
    * over the course of its lifetime.  By invoking shutdown() on the 
    * client object when the application has finished, we will not only 
    * be cleaning up the client but also removed the tracker from from 
    * tree.
    */
    void shutdown();
}

package pirc.kpi;

public interface TrackerClient {
   /**
    * TrackerClients are able to detach from the tree and clean up their
    * own internal actor but not have an effect on the Tracker that is in 
    * the tree.  This would be done in applications that want to connect and
    * dump a message into a Tracker but dont' need to hand on to the 
    * tracker any more.
    */
    void detach();

   /** 
    * This method should be called when a TrackerClient would like to not 
    * only shut itself down but also to remove from the tree that Tracker
    * that it had initially bound to.
    */
    void shutdown();
}

package pirc.kpi;

public interface CounterTrackerClient extends TrackerClient {
    void bump(Integer amt);
}

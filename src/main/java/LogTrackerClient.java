package pirc.kpi;

public interface LogTrackerClient extends TrackerClient {
    void info(String msg);
    void warning(String msg);
    void error(String msg);
}

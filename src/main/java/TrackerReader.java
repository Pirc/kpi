package pirc.kpi;

public interface TrackerReader {
    public String list();
    public String status();
    public void execute(String fn);
}

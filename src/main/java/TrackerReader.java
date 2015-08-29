package pirc.kpi;

public interface TrackerReader {
    public String list();
    public String status();
    public String execute(String fn);
}

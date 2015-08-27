package pirc.kpi;

public interface TrackerApi {
    <A extends TrackerClient> A locate(Class<A> clazz, String path);
}

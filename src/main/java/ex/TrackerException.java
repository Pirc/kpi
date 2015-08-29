package pirc.kpi.ex;

public class TrackerException extends RuntimeException {
  private Object reason;

  public TrackerException(Object o) {
    reason = o;
  }

  public Object getReason() {
    return reason;
  }
}

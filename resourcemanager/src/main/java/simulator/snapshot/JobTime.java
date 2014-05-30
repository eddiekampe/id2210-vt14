package simulator.snapshot;

/**
 * Created by eddkam on 30/05/14.
 */
public class JobTime implements Comparable<JobTime> {

    private final long startTime;
    private long endTime;

    public JobTime(long startTime) {

        this.startTime = startTime;
    }

    public void setEndTime(long endtime) {
        this.endTime = endtime;
    }

    public long getStartTime() {
        return startTime;
    }

    public long getFinalTime() {
        return endTime - startTime;
    }

    @Override
    public int compareTo(JobTime o) {

        if (startTime > o.startTime) {
            return 1;
        } else if (startTime < o.startTime) {
            return -1;
        }
        return 0;
    }
}

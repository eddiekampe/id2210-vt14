package resourcemanager.system.peer.rm;

import se.sics.kompics.timer.ScheduleTimeout;
import se.sics.kompics.timer.Timeout;

/**
 * id2210-vt14 - resourcemanager.system.peer.rm
 * User: eddkam
 * Date: 5/19/14
 */
public class ReleaseReservedResources extends Timeout {

    private final long jobId;
    private final int numCpus;
    private final int memInMb;

    protected ReleaseReservedResources(ScheduleTimeout request, long jobId, int numCpus, int memInMb) {
        super(request);
        this.jobId = jobId;
        this.numCpus = numCpus;
        this.memInMb = memInMb;
    }

    public long getJobId() {
        return jobId;
    }
}
package resourcemanager.system.peer.rm;

import se.sics.kompics.timer.ScheduleTimeout;
import se.sics.kompics.timer.Timeout;

/**
 * id2210-vt14 - resourcemanager.system.peer.rm
 * User: eddkam
 * Date: 5/20/14
 */
public class ReleaseAllocatedResources extends Timeout {

    private final int numCpus;
    private final int memInMb;

    protected ReleaseAllocatedResources(ScheduleTimeout request, int numCpus, int memInMb) {
        super(request);
        this.numCpus = numCpus;
        this.memInMb = memInMb;
    }

    public int getNumCpus() {
        return numCpus;
    }

    public int getMemInMb() {
        return memInMb;
    }

    @Override
    public String toString() {
        return "(" + numCpus + ", " + memInMb + ")";
    }
}

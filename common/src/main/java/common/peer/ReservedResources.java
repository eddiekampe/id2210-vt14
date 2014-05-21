package common.peer;

/**
 * id2210-vt14 - resourcemanager.system.peer.rm
 * User: eddkam
 * Date: 5/19/14
 */
public class ReservedResources {

    private final long jobId;
    private final int numCpus;
    private final int memInMb;

    public ReservedResources(long jobId, int numCpus, int memInMb) {

        this.jobId = jobId;
        this.numCpus = numCpus;
        this.memInMb = memInMb;
    }

    public long getJobId() {
        return jobId;
    }

    public int getNumCpus() {
        return numCpus;
    }

    public int getMemInMb() {
        return memInMb;
    }
}

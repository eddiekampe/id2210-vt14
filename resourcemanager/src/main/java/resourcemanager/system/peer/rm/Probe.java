package resourcemanager.system.peer.rm;

import se.sics.kompics.address.Address;
import se.sics.kompics.network.Message;

/**
 * id2210-vt14 - resourcemanager.system.peer.rm
 * User: eddkam
 * Date: 5/4/14
 */
public class Probe {

    public static class Request extends Message {

        private final long jobId;
        private final int numCpus;
        private final int amountMemInMb;

        public Request(Address source, Address destination, long jobId, int numCpus, int amountMemInMb) {
            super(source, destination);
            this.jobId = jobId;
            this.numCpus = numCpus;
            this.amountMemInMb = amountMemInMb;
        }

        public long getJobId() {
            return jobId;
        }

        public int getAmountMemInMb() {
            return amountMemInMb;
        }

        public int getNumCpus() {
            return numCpus;
        }

        @Override
        public String toString() {
            return "#" + jobId + " (" + numCpus + " + " + amountMemInMb + ")";
        }
    }

    public static class Nack extends Message {

        private final long jobId;

        public Nack(Address source, Address destination, long jobId) {
            super(source, destination);
            this.jobId = jobId;
        }

        public long getJobId() {
            return jobId;
        }
    }

    public static class Ack extends Message {

        private final long jobId;

        public Ack(Address source, Address destination, long jobId) {
            super(source, destination);
            this.jobId = jobId;
        }

        public long getJobId() {
            return jobId;
        }
    }
}
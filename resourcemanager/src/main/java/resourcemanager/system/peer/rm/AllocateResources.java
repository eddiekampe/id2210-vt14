package resourcemanager.system.peer.rm;

import se.sics.kompics.address.Address;
import se.sics.kompics.network.Message;

/**
 * id2210-vt14 - resourcemanager.system.peer.rm
 * User: eddkam
 * Date: 5/26/14
 */
public class AllocateResources {

    public static class Request extends Message {

        private final long jobId;
        private final int numCpus;
        private final int amountMemInMb;
        private final int timeToHold;

        public Request(Address source, Address destination, long jobId, int numCpus, int amountMemInMb, int timeToHold) {

            super(source, destination);
            this.jobId = jobId;
            this.numCpus = numCpus;
            this.amountMemInMb = amountMemInMb;
            this.timeToHold = timeToHold;
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

        public int getTimeToHold() {
            return timeToHold;
        }

        @Override
        public String toString() {
            return getNumCpus() + " + " + getAmountMemInMb();
        }
    }

    public static class Response extends Message {

        private final long jobId;
        private final boolean allocationWassuccessful;

        public Response(Address source, Address destination, long jobId, boolean allocationWassuccessful) {
            super(source, destination);
            this.jobId = jobId;
            this.allocationWassuccessful = allocationWassuccessful;
        }

        public long getJobId() {
            return jobId;
        }

        public boolean wasSuccessful() {
            return allocationWassuccessful;
        }
    }
}

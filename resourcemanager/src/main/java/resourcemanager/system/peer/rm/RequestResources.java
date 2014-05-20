package resourcemanager.system.peer.rm;

import java.util.List;
import se.sics.kompics.address.Address;
import se.sics.kompics.network.Message;
import se.sics.kompics.timer.ScheduleTimeout;
import se.sics.kompics.timer.Timeout;

/**
 * User: jdowling
 */
public class RequestResources  {

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

        public int getAmountMemInMb() {
            return amountMemInMb;
        }

        public int getNumCpus() {
            return numCpus;
        }

        @Override
        public String toString() {
            return getNumCpus() + " + " + getAmountMemInMb();
        }

        public long getJobId() {
            return jobId;
        }

        public int getTimeToHold() {
            return timeToHold;
        }
    }
    
    public static class Response extends Message {

        private final boolean success;
        private final long jobId;
        public Response(Address source, Address destination, boolean success, long jobId) {
            super(source, destination);
            this.success = success;
            this.jobId = jobId;
        }

        public boolean isSuccess() {
            return success;
        }

        public long getJobId() {
            return jobId;
        }
    }
    
    public static class RequestTimeout extends Timeout {
        private final Address destination;
        RequestTimeout(ScheduleTimeout st, Address destination) {
            super(st);
            this.destination = destination;
        }

        public Address getDestination() {
            return destination;
        }
    }
}

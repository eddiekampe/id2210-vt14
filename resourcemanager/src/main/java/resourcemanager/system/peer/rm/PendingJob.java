package resourcemanager.system.peer.rm;

import se.sics.kompics.address.Address;

import java.util.ArrayList;
import java.util.List;

/**
 * id2210-vt14 - resourcemanager.system.peer.rm
 * User: eddkam
 * Date: 5/19/14
 */
public class PendingJob {

    private final List<Address> addresses;
    private List<Address> ackWorkers;
    private int acc;

    public PendingJob(List<Address> addresses) {

        this.acc = 0;
        this.addresses = new ArrayList(addresses);
        this.ackWorkers = new ArrayList<Address>();
    }

    public void addNack() {
        this.acc++;
    }

    public void addAck(Address address) {
        ackWorkers.add(address);
        this.acc++;
    }

    public int allResponsesReceived() {

        if (acc != addresses.size()) {
            return 0;
        } else if (ackWorkers.size() > 0){
            return 1;
        } else {
            return -1;
        }
    }

    /**
     * Very naive ranking function
     * @return Best available worker
     */
    public Address getBestWorker() {
        return addresses.get(0);
    }
}

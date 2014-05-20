package common.peer;

import se.sics.kompics.address.Address;
import se.sics.kompics.p2p.overlay.OverlayAddress;

/**
 * This class contains information about the Peer.
 *
 * @author jdowling
 */
public final class PeerDescriptor extends OverlayAddress implements Comparable<PeerDescriptor> {

    private static final long serialVersionUID = -7582889514221620065L;

    private final int numFreeCpus;
    private final int freeMemoryInMbs;

    public PeerDescriptor(Address address, int numFreeCpus, int freeMemoryInMbs) {
        super(address);
        this.numFreeCpus = numFreeCpus;
        this.freeMemoryInMbs = freeMemoryInMbs;
    }

    public int getFreeMemoryInMbs() {
        return freeMemoryInMbs;
    }

    public int getNumFreeCpus() {
        return numFreeCpus;
    }
    
    

    @Override
    public int compareTo(PeerDescriptor that) {
        if (id() > that.getPeerAddress().getId()) {
            return 1;
        }
        return -1;
    }

    private int id() {
        return this.getPeerAddress().getId();
    }

    @Override
    public String toString() {
        return Integer.toString(id());
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = super.hashCode();
        result = prime * result
                + (id());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!super.equals(obj)) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        PeerDescriptor other = (PeerDescriptor) obj;
        return id() == other.id();
    }
}

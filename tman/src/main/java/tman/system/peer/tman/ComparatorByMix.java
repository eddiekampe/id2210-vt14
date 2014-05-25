package tman.system.peer.tman;

import cyclon.system.peer.cyclon.PeerDescriptor;

import java.util.Comparator;

/**
 * id2210-vt14 - tman.system.peer.tman
 * User: eddkam
 * Date: 5/21/14
 */
public class ComparatorByMix implements Comparator<PeerDescriptor> {

    public ComparatorByMix() {
    }

    @Override
    public int compare(PeerDescriptor p1, PeerDescriptor p2) {

        //assert (p1.getAddress().getId() == p2.getAddress().getId());
        Float v1 = calculateUtility(p1);
        Float v2 = calculateUtility(p2);
        return v2.compareTo(v1);
    }

    /**
     * Calculates a peer's utility value
     *
     * @param p Peer
     * @return Utility value
     */
    private float calculateUtility(PeerDescriptor p) {

        int numFreeCpus = p.getNumFreeCpus();
        int freeMemoryInMbs = p.getFreeMemoryInMbs();
        if (freeMemoryInMbs == 0) {
            freeMemoryInMbs = 1;
        }
        return numFreeCpus + (1.0f - 1.0f / freeMemoryInMbs);
    }
}

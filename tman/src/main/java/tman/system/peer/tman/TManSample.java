package tman.system.peer.tman;

import java.util.*;


import cyclon.system.peer.cyclon.PeerDescriptor;
import se.sics.kompics.Event;
import se.sics.kompics.address.Address;


public class TManSample extends Event {

    Set<PeerDescriptor> nodes = new HashSet<PeerDescriptor>();

	public TManSample(Set<PeerDescriptor> partners) {
		this.nodes = partners;
	}
        
	public TManSample() { }


	public List<PeerDescriptor> getSample() {
        List<PeerDescriptor> sortedPeers = new ArrayList<PeerDescriptor>(nodes);
        Collections.sort(sortedPeers, new ComparatorByMix());
		return sortedPeers;
	}

    public ArrayList<Address> getAddressSample() {
        ArrayList<Address> partners = new ArrayList<Address>();
        for (PeerDescriptor node : nodes) {
            partners.add(node.getAddress());
        }
        return partners;
    }
}

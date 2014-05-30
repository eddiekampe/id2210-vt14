package tman.system.peer.tman;

import java.util.*;


import cyclon.system.peer.cyclon.PeerDescriptor;
import se.sics.kompics.Event;
import se.sics.kompics.address.Address;


public class TManSample extends Event {

    private List<PeerDescriptor> nodes = new ArrayList<PeerDescriptor>();

	public TManSample(List<PeerDescriptor> partners) {
		this.nodes = partners;
	}
        
	public TManSample() { }


	public List<PeerDescriptor> getSample() {
        return new ArrayList<PeerDescriptor>(nodes);
	}

    public ArrayList<Address> getAddressSample() {
        ArrayList<Address> partners = new ArrayList<Address>();
        for (PeerDescriptor node : nodes) {
            partners.add(node.getAddress());
        }
        return partners;
    }
}

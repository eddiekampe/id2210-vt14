package cyclon.system.peer.cyclon;

import se.sics.kompics.Event;
import se.sics.kompics.address.Address;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;


public class CyclonSample extends Event {

    private Set<PeerDescriptor> nodes = new HashSet<PeerDescriptor>();

	public CyclonSample(List<PeerDescriptor> nodes) {
		this.nodes = new HashSet<PeerDescriptor>(nodes);
	}

	public CyclonSample() { }

	public Set<PeerDescriptor> getSample() {
		return this.nodes;
	}

    public ArrayList<Address> getAddressSample() {
        ArrayList<Address> partners = new ArrayList<Address>();
        for (PeerDescriptor node : nodes) {
            partners.add(node.getAddress());
        }
        return partners;
    }
}
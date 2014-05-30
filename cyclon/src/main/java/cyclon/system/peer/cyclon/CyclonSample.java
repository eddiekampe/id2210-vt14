package cyclon.system.peer.cyclon;

import se.sics.kompics.Event;
import se.sics.kompics.address.Address;

import java.util.ArrayList;
import java.util.List;


public class CyclonSample extends Event {

    private List<PeerDescriptor> nodes = new ArrayList<PeerDescriptor>();

	public CyclonSample(List<PeerDescriptor> nodes) {
		this.nodes = new ArrayList<PeerDescriptor>(nodes);
	}

	public CyclonSample() { }

	public List<PeerDescriptor> getSample() {
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
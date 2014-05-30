package cyclon.system.peer.cyclon;

import se.sics.kompics.Event;

import java.util.ArrayList;


public class CyclonPartnersResponse extends Event {

	ArrayList<PeerDescriptor> partners = new ArrayList<PeerDescriptor>();

	public CyclonPartnersResponse(ArrayList<PeerDescriptor> partners) {
		this.partners = partners;
	}
        
	public CyclonPartnersResponse() { }


	public ArrayList<PeerDescriptor> getPartners() {
		return this.partners;
	}
}

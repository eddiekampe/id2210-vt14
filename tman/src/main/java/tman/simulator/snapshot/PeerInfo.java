package tman.simulator.snapshot;

import cyclon.system.peer.cyclon.PeerDescriptor;

import java.util.ArrayList;


public class PeerInfo {
	private ArrayList<PeerDescriptor> tmanPartners;
	private ArrayList<PeerDescriptor> cyclonPartners;


	public PeerInfo() {
		this.tmanPartners = new ArrayList<PeerDescriptor>();
		this.cyclonPartners = new ArrayList<PeerDescriptor>();
	}


	public void updateTManPartners(ArrayList<PeerDescriptor> partners) {
		this.tmanPartners = partners;
	}


	public void updateCyclonPartners(ArrayList<PeerDescriptor> partners) {
		this.cyclonPartners = partners;
	}


	public ArrayList<PeerDescriptor> getTManPartners() {
		return this.tmanPartners;
	}


	public ArrayList<PeerDescriptor> getCyclonPartners() {
		return this.cyclonPartners;
	}
}

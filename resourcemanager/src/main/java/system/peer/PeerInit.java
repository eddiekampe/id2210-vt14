package system.peer;

import common.configuration.RmConfiguration;
import common.configuration.CyclonConfiguration;
import common.configuration.TManConfiguration;
import common.peer.AvailableResources;
import se.sics.kompics.Init;
import se.sics.kompics.address.Address;
import se.sics.kompics.p2p.bootstrap.BootstrapConfiguration;

public final class PeerInit extends Init {

    private final Address peerSelf;
    private final BootstrapConfiguration bootstrapConfiguration;
    private final CyclonConfiguration cyclonConfiguration;
    private final TManConfiguration tManConfiguration;
    private final RmConfiguration applicationConfiguration;
    private final AvailableResources availableResources;

    public PeerInit(Address peerSelf, BootstrapConfiguration bootstrapConfiguration,
            CyclonConfiguration cyclonConfiguration, TManConfiguration tManConfiguration, RmConfiguration applicationConfiguration,
            AvailableResources availableResources) {
        super();
        this.peerSelf = peerSelf;
        this.bootstrapConfiguration = bootstrapConfiguration;
        this.cyclonConfiguration = cyclonConfiguration;
        this.tManConfiguration = tManConfiguration;
        this.applicationConfiguration = applicationConfiguration;
        this.availableResources = availableResources;

    }

    public AvailableResources getAvailableResources() {
        return availableResources;
    }

    public Address getPeerSelf() {
        return this.peerSelf;
    }

    public BootstrapConfiguration getBootstrapConfiguration() {
        return this.bootstrapConfiguration;
    }

    public CyclonConfiguration getCyclonConfiguration() {
        return this.cyclonConfiguration;
    }

    public RmConfiguration getApplicationConfiguration() {
        return this.applicationConfiguration;
    }

    public TManConfiguration getTManConfiguration() {
        return tManConfiguration;
    }
}
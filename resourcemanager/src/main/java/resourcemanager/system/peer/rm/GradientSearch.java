package resourcemanager.system.peer.rm;

import common.simulation.RequestResource;
import se.sics.kompics.address.Address;
import se.sics.kompics.network.Message;

/**
 *
 * @author kevin yeoh
 */
public class GradientSearch extends Message {

    private RequestResource req;

    public GradientSearch(Address source, Address dest, RequestResource req) {

        super(source, dest);
        this.req = req;
    }

    /**
     * @return the req
     */
    public RequestResource getReq() {
        return req;
    }
}

package tman.system.peer.tman;

import java.util.*;

import cyclon.system.peer.cyclon.PeerDescriptor;
import se.sics.kompics.address.Address;
import se.sics.kompics.network.Message;
import se.sics.kompics.timer.ScheduleTimeout;
import se.sics.kompics.timer.Timeout;

public class ExchangeMsg {

    public static class Request extends Message {

        private static final long serialVersionUID = 8493601671018888143L;
        private final UUID requestId;
        private final List<PeerDescriptor> buffer;

        public Request(Address source, Address destination, List<PeerDescriptor> buffer) {
            super(source, destination);
            this.requestId = UUID.randomUUID();
            this.buffer = new ArrayList<PeerDescriptor>(buffer);
        }

        public UUID getRequestId() {
            return this.requestId;
        }
        
        public List<PeerDescriptor> getBuffer() {
            return this.buffer;
        }
    }

    public static class Response extends Message {

        private static final long serialVersionUID = -5022051054665787770L;
        private final UUID requestId;
        private final List<PeerDescriptor> buffer;

        public Response(Address source, Address destination, List<PeerDescriptor> buffer) {
            super(source, destination);
            this.requestId = UUID.randomUUID();
            this.buffer = new ArrayList<PeerDescriptor>(buffer);
        }


        public UUID getRequestId() {
            return requestId;
        }

        public List<PeerDescriptor> getBuffer() {
            return this.buffer;
        }
    }

    public static class RequestTimeout extends Timeout {

        private final Address peer;


        public RequestTimeout(ScheduleTimeout request, Address peer) {
            super(request);
            this.peer = peer;
        }


        public Address getPeer() {
            return peer;
        }
    }
}
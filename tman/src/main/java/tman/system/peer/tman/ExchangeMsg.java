package tman.system.peer.tman;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import cyclon.system.peer.cyclon.PeerDescriptor;
import se.sics.kompics.address.Address;
import se.sics.kompics.network.Message;
import se.sics.kompics.timer.ScheduleTimeout;
import se.sics.kompics.timer.Timeout;

public class ExchangeMsg {

    public static class Request extends Message {

        private static final long serialVersionUID = 8493601671018888143L;
        private final UUID requestId;
        private final Set<PeerDescriptor> buffer;

        public Request(Address source, Address destination, Set<PeerDescriptor> buffer) {
            super(source, destination);
            this.requestId = UUID.randomUUID();
            this.buffer = new HashSet<PeerDescriptor>(buffer);
        }

        public UUID getRequestId() {
            return this.requestId;
        }
        
        public Set<PeerDescriptor> getBuffer() {
            return this.buffer;
        }
    }

    public static class Response extends Message {

        private static final long serialVersionUID = -5022051054665787770L;
        private final UUID requestId;
        private final Set<PeerDescriptor> buffer;

        public Response(Address source, Address destination, Set<PeerDescriptor> buffer) {
            super(source, destination);
            this.requestId = UUID.randomUUID();
            this.buffer = new HashSet<PeerDescriptor>(buffer);
        }


        public UUID getRequestId() {
            return requestId;
        }

        public Set<PeerDescriptor> getBuffer() {
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
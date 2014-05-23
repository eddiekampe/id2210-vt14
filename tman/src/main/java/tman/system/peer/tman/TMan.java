package tman.system.peer.tman;

import common.configuration.TManConfiguration;

import java.util.*;

import common.peer.AvailableResources;
import cyclon.system.peer.cyclon.CyclonSample;
import cyclon.system.peer.cyclon.CyclonSamplePort;

import cyclon.system.peer.cyclon.PeerDescriptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import se.sics.kompics.ComponentDefinition;
import se.sics.kompics.Handler;
import se.sics.kompics.Negative;
import se.sics.kompics.Positive;
import se.sics.kompics.address.Address;
import se.sics.kompics.network.Network;
import se.sics.kompics.timer.SchedulePeriodicTimeout;
import se.sics.kompics.timer.ScheduleTimeout;
import se.sics.kompics.timer.Timeout;
import se.sics.kompics.timer.Timer;

import tman.simulator.snapshot.Snapshot;

public final class TMan extends ComponentDefinition {

    private static final Logger logger = LoggerFactory.getLogger(TMan.class);
    private static final int C = 8;

    Negative<TManSamplePort> tmanPort = negative(TManSamplePort.class);
    Positive<CyclonSamplePort> cyclonSamplePort = positive(CyclonSamplePort.class);
    Positive<Network> networkPort = positive(Network.class);
    Positive<Timer> timerPort = positive(Timer.class);
    private Address self;
    private Set<PeerDescriptor> tmanPartners;
    private Set<PeerDescriptor> cyclonPartners;
    private TManConfiguration tmanConfiguration;
    private Random random;
    private AvailableResources availableResources;

    public class TManSchedule extends Timeout {

        public TManSchedule(SchedulePeriodicTimeout request) {
            super(request);
        }

        public TManSchedule(ScheduleTimeout request) {
            super(request);
        }
    }

    public TMan() {

        tmanPartners = new HashSet<PeerDescriptor>();
        cyclonPartners = new HashSet<PeerDescriptor>();

        subscribe(handleInit, control);
        subscribe(handleRound, timerPort);
        subscribe(handleCyclonSample, cyclonSamplePort);
        subscribe(handleTManPartnersResponse, networkPort);
        subscribe(handleTManPartnersRequest, networkPort);
    }

    Handler<TManInit> handleInit = new Handler<TManInit>() {

        @Override
        public void handle(TManInit init) {

            self = init.getSelf();
            tmanConfiguration = init.getConfiguration();
            long period = tmanConfiguration.getPeriod();
            random = new Random(tmanConfiguration.getSeed());
            availableResources = init.getAvailableResources();

            SchedulePeriodicTimeout rst = new SchedulePeriodicTimeout(period, period);
            rst.setTimeoutEvent(new TManSchedule(rst));
            trigger(rst, timerPort);
        }
    };

    Handler<TManSchedule> handleRound = new Handler<TManSchedule>() {

        @Override
        public void handle(TManSchedule event) {

            //System.out.println("TManSchedule");
            Snapshot.updateTManPartners(self, new ArrayList<PeerDescriptor>(tmanPartners));
            // Publish sample to connected components
            trigger(new TManSample(tmanPartners), tmanPort);
        }
    };

    Handler<CyclonSample> handleCyclonSample = new Handler<CyclonSample>() {

        @Override
        public void handle(CyclonSample event) {

            //System.out.println("CyclonSample");
            int numFreeCpus = availableResources.getNumFreeCpus();
            int freeMemInMbs = availableResources.getFreeMemInMbs();
            Set<PeerDescriptor> buffer;
            cyclonPartners = event.getSample();

            Snapshot.updateCyclonPartners(self, new ArrayList<PeerDescriptor>(cyclonPartners));

            if (tmanPartners.size() == 0) {
                tmanPartners = new HashSet<PeerDescriptor>(cyclonPartners);
            }

            PeerDescriptor q = getSoftMaxAddress(new ArrayList<PeerDescriptor>(tmanPartners));
            // It could happen that no peer is available at the moment
            if (q != null) {

                PeerDescriptor myDescriptor = new PeerDescriptor(self, numFreeCpus, freeMemInMbs);
                buffer = merge(tmanPartners, myDescriptor);
                buffer = merge(buffer, cyclonPartners);
                // Start the exchange between peers
                ExchangeMsg.Request message = new ExchangeMsg.Request(self, q.getAddress(), buffer);
                trigger(message, networkPort);
            }
        }
    };


    Handler<ExchangeMsg.Request> handleTManPartnersRequest = new Handler<ExchangeMsg.Request>() {

        @Override
        public void handle(ExchangeMsg.Request event) {

            //System.out.println("ExchangeMsg.Request");
            int numFreeCpus = availableResources.getNumFreeCpus();
            int freeMemInMbs = availableResources.getFreeMemInMbs();
            Set<PeerDescriptor> buffer;

            PeerDescriptor myDescriptor = new PeerDescriptor(self, numFreeCpus, freeMemInMbs);
            buffer = merge(tmanPartners, myDescriptor);
            buffer = merge(buffer, cyclonPartners);
            // Send back response
            ExchangeMsg.Response message = new ExchangeMsg.Response(self, event.getSource(), buffer);
            trigger(message, networkPort);

            buffer = merge(event.getBuffer(), tmanPartners);
            tmanPartners = new HashSet<PeerDescriptor>(buffer);
        }
    };

    Handler<ExchangeMsg.Response> handleTManPartnersResponse = new Handler<ExchangeMsg.Response>() {

        @Override
        public void handle(ExchangeMsg.Response event) {

            //System.out.println("ExchangeMsg.Response");
            Set<PeerDescriptor> buffer;
            buffer = merge(event.getBuffer(), tmanPartners);

            // Sort and keep C highest peers
            List<PeerDescriptor> sortedPeers = new ArrayList<PeerDescriptor>(buffer);
            Collections.sort(sortedPeers, new ComparatorByMix());
            tmanPartners = new HashSet<PeerDescriptor>(sortedPeers.subList(0, Math.min(C, sortedPeers.size())));
        }
    };


    /**
     * Return a single node, weighted towards the 'best' node (as defined by
     * ComparatorById) with the temperature controlling the weighting.
     *
     * A temperature of '1.0' will be greedy and always return the best node.
     * A temperature of '0.000001' will return a random node.
     * A temperature of '0.0' will throw a divide by zero exception :)
     *
     * @param entries List of PeerDescriptors
     * @return Single descriptor based on Comparator and temperature
     */
    private PeerDescriptor getSoftMaxAddress(List<PeerDescriptor> entries) {

        if (entries.size() == 0) {
            return null;
        }

        Collections.sort(entries, new ComparatorByMix());

        double rnd = random.nextDouble();
        double total = 0.0d;
        double[] values = new double[entries.size()];
        int j = entries.size() + 1;
        for (int i = 0; i < entries.size(); i++) {
            // Get inverse of values - lowest have highest value.
            double val = j;
            j--;
            values[i] = Math.exp(val / tmanConfiguration.getTemperature());
            total += values[i];
        }

        for (int i = 0; i < values.length; i++) {
            if (i != 0) {
                values[i] += values[i - 1];
            }
            // Normalise the probability for this entry
            double normalisedUtility = values[i] / total;
            if (normalisedUtility >= rnd) {
                return entries.get(i);
            }
        }
        return entries.get(entries.size() - 1);
    }

    /**
     * Merge two Sets of PeerDescriptors together
     * @param p1 Set 1
     * @param p2 Set 2
     * @return Merged Set
     */
    private Set<PeerDescriptor> merge(Set<PeerDescriptor> p1, Set<PeerDescriptor> p2) {

        Set<PeerDescriptor> result = new HashSet<PeerDescriptor>(p1);
        result.addAll(p2);
        return result;
    }

    /**
     * Merge a Set of PeerDescriptor with a single Descriptor
     * (Basically add)
     * @param p1 Set 1
     * @param p2 PeerDescriptor
     * @return Merged Set
     */
    private Set<PeerDescriptor> merge(Set<PeerDescriptor> p1, PeerDescriptor p2) {

        Set<PeerDescriptor> result = new HashSet<PeerDescriptor>(p1);
        result.add(p2);
        return result;
    }
}

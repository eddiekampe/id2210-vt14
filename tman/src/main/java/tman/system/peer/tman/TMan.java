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
    private static final int C = 12;

    Negative<TManSamplePort> tmanPort = negative(TManSamplePort.class);
    Positive<CyclonSamplePort> cyclonSamplePort = positive(CyclonSamplePort.class);
    Positive<Network> networkPort = positive(Network.class);
    Positive<Timer> timerPort = positive(Timer.class);
    private Address self;
    private ArrayList<PeerDescriptor> tmanPartners;
    private List<PeerDescriptor> cyclonPartners;
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

        tmanPartners = new ArrayList<PeerDescriptor>();
        cyclonPartners = new ArrayList<PeerDescriptor>();

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
            Snapshot.updateTManPartners(self, tmanPartners);
            // Publish sample to connected components
            tmanPartners = new ArrayList<PeerDescriptor>(tmanPartners.subList(0, Math.min(C, tmanPartners.size())));
            trigger(new TManSample(tmanPartners), tmanPort);
        }
    };

    Handler<CyclonSample> handleCyclonSample = new Handler<CyclonSample>() {

        @Override
        public void handle(CyclonSample event) {
            int numFreeCpus = availableResources.getNumFreeCpus();
            int freeMemInMbs = availableResources.getFreeMemInMbs();
            List<PeerDescriptor> buffer;
            cyclonPartners = event.getSample();

            Snapshot.updateCyclonPartners(self, new ArrayList<PeerDescriptor>(cyclonPartners));

            /*for (int i = 0; i < cyclonPartners.size(); i++) {
             logger.info(self + " receiving cyclon samples: " + cyclonPartners.get(i).getAddress().toString());
             }*/
            if (tmanPartners.isEmpty()) {
                tmanPartners = new ArrayList<PeerDescriptor>(cyclonPartners);
            }
            /*for (int i = 0; i < tmanPartners.size(); i++) {
             logger.info(self + " beforeee SoftMax samples: " + tmanPartners.get(i).getAddress().toString());
             }*/
            PeerDescriptor q = getSoftMaxAddress(tmanPartners);
            /*for (int i = 0; i < tmanPartners.size(); i++) {
             logger.info(self + " SoftMax samples: " + tmanPartners.get(i).getAddress().toString());
             }*/

            // It could happen that no peer is available at the moment
            if (q != null) {

                PeerDescriptor myDescriptor = new PeerDescriptor(self, numFreeCpus, freeMemInMbs, System.currentTimeMillis());
                buffer = merge(tmanPartners, myDescriptor);
                buffer = merge(buffer, cyclonPartners);
                /*for (int i = 0; i < tmanPartners.size(); i++) {
                 logger.info(self + " buffer samples: " + tmanPartners.get(i).getAddress().toString());
                 }*/

                // Start the exchange between peers
                if (!self.equals(q.getAddress())) {
                    // we have to order these nodes if we send it to RM
                    Collections.sort(tmanPartners, new ComparatorByMix());
                    /*for (int i = 0; i < tmanPartners.size(); i++) {
                     logger.info(self + " request TMAN samples: " + tmanPartners.get(i).getAddress().toString());
                     }*/
                    ExchangeMsg.Request message = new ExchangeMsg.Request(self, q.getAddress(), buffer);
                    trigger(message, networkPort);
                }
            }
        }
    };

    Handler<ExchangeMsg.Request> handleTManPartnersRequest = new Handler<ExchangeMsg.Request>() {

        @Override
        public void handle(ExchangeMsg.Request event) {

            //System.out.println("ExchangeMsg.Request");
            int numFreeCpus = availableResources.getNumFreeCpus();
            int freeMemInMbs = availableResources.getFreeMemInMbs();
            List<PeerDescriptor> buffer;

            PeerDescriptor myDescriptor = new PeerDescriptor(self, numFreeCpus, freeMemInMbs, System.currentTimeMillis());
            buffer = merge(tmanPartners, myDescriptor);
            buffer = merge(buffer, cyclonPartners);
            // sort everytime we send out our view
            Collections.sort(buffer, new ComparatorByMix());
            /*for (int i = 0; i < tmanPartners.size(); i++) {
             logger.info(self + " reply TMAN samples: " + tmanPartners.get(i).getAddress().toString());
             }*/
            // Send back response
            ExchangeMsg.Response message = new ExchangeMsg.Response(self, event.getSource(), buffer);
            trigger(message, networkPort);

            // merge the received buffer
            buffer = merge(event.getBuffer(), tmanPartners);
            // and sort them
            Collections.sort(buffer, new ComparatorByMix());
            tmanPartners = new ArrayList<PeerDescriptor>(buffer);
        }
    };

    Handler<ExchangeMsg.Response> handleTManPartnersResponse = new Handler<ExchangeMsg.Response>() {

        @Override
        public void handle(ExchangeMsg.Response event) {

            //System.out.println("ExchangeMsg.Response");
            List<PeerDescriptor> buffer;
            buffer = merge(event.getBuffer(), tmanPartners);

            // Sort and keep C highest peers
            Collections.sort(buffer, new ComparatorByMix());
            tmanPartners = new ArrayList<PeerDescriptor>(buffer.subList(0, Math.min(C, buffer.size())));
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
     *
     * @param p1 Set 1
     * @param p2 Set 2
     * @return Merged Set
     */
    private List<PeerDescriptor> merge(List<PeerDescriptor> p1, List<PeerDescriptor> p2) {

        List<PeerDescriptor> duplicates = new LinkedList<PeerDescriptor>(p1);
        List<PeerDescriptor> result = new LinkedList<PeerDescriptor>();
        /*
        logger.info("-----------------------------------");
        for (int i = 0; i < p1.size(); i++) {
            logger.info("P1 before merge: " + p1.get(i));
        }
        for (int i = 0; i < p2.size(); i++) {
            logger.info("P 2 before merge: " + p2.get(i));
        }
        */
        // Keep only duplicates
        duplicates.retainAll(p2);
        // Add descriptors to result
        for (PeerDescriptor pd1 : p1) {

            if (duplicates.contains(pd1)) {
                // Decide which to keep
                PeerDescriptor pd2 = p2.get(p2.indexOf(pd1));
                if (pd1.compareTo(pd2) > 0) {
                    result.add(pd1);
                } else {
                    result.add(pd2);
                }
            } else {
                result.add(pd1);
            }
        }
        /*
        for (int i = 0; i < result.size(); i++) {
            logger.info("Result set: " + result.get(i));
        }
        logger.info("-----------------------------------");
        */
        return result;
    }

    /**
     * Merge a Set of PeerDescriptor with a single Descriptor (Basically add)
     *
     * @param p1 Set 1
     * @param p2 PeerDescriptor
     * @return Merged Set
     */
    private List<PeerDescriptor> merge(List<PeerDescriptor> p1, PeerDescriptor p2) {

        /*
        logger.info("-----------------------------------");
        for (int i = 0; i < p1.size(); i++) {
            logger.info("P1 before merge: " + p1.get(i));
        }
        logger.info("P2: " + p2);
        */
        // Basic case, p2 not in p1
        if (!p1.contains(p2)) {
            p1.add(p2);
            return p1;
        }

        // P2 already in P1, check which one to keep
        PeerDescriptor pd1 = p1.get(p1.indexOf(p2));
        if (pd1.compareTo(p2) > 0) {

        } else {
            p1.remove(pd1);
            p1.add(p2);
        }
        /*
        for (int i = 0; i < p1.size(); i++) {
            logger.info("After single merge: " + p1.get(i));
        }
        logger.info("-----------------------------------");
        */
        return p1;
    }
}

package resourcemanager.system.peer.rm;

import common.configuration.RmConfiguration;
import common.peer.AvailableResources;
import common.simulation.RequestResource;
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
import se.sics.kompics.timer.Timer;
import se.sics.kompics.web.Web;
import system.peer.RmPort;
import tman.system.peer.tman.TManSample;
import tman.system.peer.tman.TManSamplePort;

import java.util.*;
import tman.system.peer.tman.ComparatorByMix;

/**
 * Should have some comments here.
 *
 * @author jdowling
 */
public final class ResourceManager extends ComponentDefinition {

    private static final Logger logger = LoggerFactory.getLogger(ResourceManager.class);
    private static final int nPeersToProbe = 2;
    private static final int reservedTimeout = 2000;
    private static boolean useImprovedSparrow;

    Positive<RmPort> indexPort = positive(RmPort.class);
    Positive<Network> networkPort = positive(Network.class);
    Positive<Timer> timerPort = positive(Timer.class);
    Negative<Web> webPort = negative(Web.class);
    Positive<CyclonSamplePort> cyclonSamplePort = positive(CyclonSamplePort.class);
    Positive<TManSamplePort> tmanPort = positive(TManSamplePort.class);

    private Address self;
    private ArrayList<PeerDescriptor> neighbours = new ArrayList<PeerDescriptor>();
    private Random random;
    private RmConfiguration configuration;
    private AvailableResources availableResources;
    private Map<RequestResource, PendingJob> ongoingJobs;
    private List<RequestResource> jobList;

    /**
     * Bind handlers to ports.
     */
    public ResourceManager() {

        subscribe(handleInit, control);
        subscribe(handleCyclonSample, cyclonSamplePort);
        subscribe(handleRequestResource, indexPort);
        subscribe(handleUpdateTimeout, timerPort);
        subscribe(handleResourceAllocationRequest, networkPort);
        subscribe(handleResourceAllocationResponse, networkPort);
        subscribe(handleProbeRequest, networkPort);
        subscribe(handleTManSample, tmanPort);
        subscribe(handleReservationTimeout, timerPort);
        subscribe(handleAllocationTimeout, timerPort);
        subscribe(handleProbeAck, networkPort);
        subscribe(handleProbeNack, networkPort);
        subscribe(handleGradientSearch, networkPort);
    }

    /**
     * Initialize ResourceManager
     */
    Handler<RmInit> handleInit = new Handler<RmInit>() {
        @Override
        public void handle(RmInit init) {

            ongoingJobs = new HashMap<RequestResource, PendingJob>();
            self = init.getSelf();
            configuration = init.getConfiguration();
            useImprovedSparrow = configuration.useImprovedSparrow();
            random = new Random(init.getConfiguration().getSeed());
            availableResources = init.getAvailableResources();
            jobList = new ArrayList<RequestResource>();
            long period = configuration.getPeriod();
            SchedulePeriodicTimeout periodicTimeout = new SchedulePeriodicTimeout(period, period);
            periodicTimeout.setTimeoutEvent(new UpdateTimeout(periodicTimeout));
            trigger(periodicTimeout, timerPort);
        }
    };

    /**
     * Update handler, running periodically based on ResourceManager
     * configuration (RmConfiguration)
     */
    Handler<UpdateTimeout> handleUpdateTimeout = new Handler<UpdateTimeout>() {
        @Override
        public void handle(UpdateTimeout event) {

            //System.out.println("handleUpdateTimeout: " + neighbours.size());
            // pick a random neighbour to ask for index updates from. 
            // You can change this policy if you want to.
            // Maybe a gradient neighbour who is closer to the leader?
            if (neighbours.isEmpty()) {
                return;
            }
            PeerDescriptor dest = neighbours.get(random.nextInt(neighbours.size()));
        }
    };

    /**
     * Handle incoming message CyclonSample. Update list of neighbours
     */
    Handler<CyclonSample> handleCyclonSample = new Handler<CyclonSample>() {
        @Override
        public void handle(CyclonSample event) {

            if (!useImprovedSparrow) {
                logger.debug("Received samples: " + event.getSample().size());
                neighbours.clear();
                neighbours.addAll(event.getSample());
            }
        }
    };

    /**
     * Handle incoming message TManSample. What to do?
     */
    Handler<TManSample> handleTManSample = new Handler<TManSample>() {
        @Override
        public void handle(TManSample event) {

            if (useImprovedSparrow) {
                //logger.info("TManSample: " + event.getSample().size());
                /*for (int i = 0; i < event.getSample().size(); i++) {
                 logger.info(i + " " + event.getSample().get(i).getAddress().toString() + " ,CPU: "
                 + event.getSample().get(i).getNumFreeCpus() + " ,MEM: "
                 + event.getSample().get(i).getFreeMemoryInMbs());
                 }*/
                neighbours.clear();
                neighbours.addAll(event.getSample());
            }
        }
    };

    /**
     * Handle incoming resource allocation request. Should we accept the
     * request? Do we have enough resources?
     */
    Handler<RequestResources.Request> handleResourceAllocationRequest = new Handler<RequestResources.Request>() {
        @Override
        public void handle(RequestResources.Request event) {

            long jobId = event.getJobId();
            boolean successfullyAllocatedResources = availableResources.allocate(jobId);
            RequestResources.Response resp = new RequestResources.Response(self, event.getSource(), successfullyAllocatedResources, jobId);
            trigger(resp, networkPort);

            if (successfullyAllocatedResources) {
                // Trigger the timeout for holding the resources
                ScheduleTimeout timeout = new ScheduleTimeout(event.getTimeToHold());
                timeout.setTimeoutEvent(new ReleaseAllocatedResources(timeout, event.getNumCpus(), event.getAmountMemInMb()));
                trigger(timeout, timerPort);
            }
        }
    };

    /**
     * Handle incoming resource allocation response. Did we get the requested
     * resources?
     */
    Handler<RequestResources.Response> handleResourceAllocationResponse = new Handler<RequestResources.Response>() {
        @Override
        public void handle(RequestResources.Response event) {

            if (event.isSuccess()) {
                logger.info("Allocation successful on " + event.getSource());
            } else {
                long jobId = event.getJobId();
                for (Map.Entry<RequestResource, PendingJob> jobEntry : ongoingJobs.entrySet()) {

                    long ongoingJobId = jobEntry.getKey().getId();
                    if (ongoingJobId == jobId) {
                        ongoingJobs.remove(jobEntry.getKey());
                        logger.info("Experied ACK: " + event.getSource());
                        scheduleJob(jobEntry.getKey());
                        break;
                    }
                }
            }
        }
    };

    /**
     * Schedule incoming resource request.
     */
    Handler<RequestResource> handleRequestResource = new Handler<RequestResource>() {
        @Override
        public void handle(RequestResource jobEvent) {
            logger.info(self + " handleRequestResource Schedule");
            scheduleJob(jobEvent);
        }
    };

    Handler<GradientSearch> handleGradientSearch = new Handler<GradientSearch>() {
        @Override
        public void handle(GradientSearch jobEvent) {
            logger.info(self + " handleGradientSearch Schedule from " + jobEvent.getSource());
            scheduleJob(jobEvent.getReq());
        }
    };

    /**
     * Handle incoming probe request. Respond with available resources.
     */
    Handler<Probe.Request> handleProbeRequest = new Handler<Probe.Request>() {
        @Override
        public void handle(Probe.Request request) {

            logger.debug("Got Probe.Request");

            Address requester = request.getSource();
            long jobId = request.getJobId();
            int numCpus = request.getNumCpus();
            int amountMemInMb = request.getAmountMemInMb();

            boolean resourcesAreAvailable = availableResources.reserveIfAvailable(jobId, numCpus, amountMemInMb);

            if (resourcesAreAvailable) {
                // ACK
                ScheduleTimeout timeout = new ScheduleTimeout(reservedTimeout);
                timeout.setTimeoutEvent(new ReleaseReservedResources(timeout, jobId, numCpus, amountMemInMb));
                trigger(timeout, timerPort);

                Probe.Ack response = new Probe.Ack(self, requester, jobId);
                trigger(response, networkPort);

            } else {
                // NACK
                Probe.Nack response = new Probe.Nack(self, requester, jobId);
                trigger(response, networkPort);
            }
        }
    };

    /**
     * Handles the timeout of a reserved resource
     */
    Handler<ReleaseReservedResources> handleReservationTimeout = new Handler<ReleaseReservedResources>() {

        @Override
        public void handle(ReleaseReservedResources releaseEvent) {
            long jobId = releaseEvent.getJobId();
            availableResources.releaseReservedResources(jobId);
            logger.debug("Release reserved resources: " + jobId);
        }
    };

    /**
     * Handles the timeout of a reserved resource
     */
    Handler<ReleaseAllocatedResources> handleAllocationTimeout = new Handler<ReleaseAllocatedResources>() {
        @Override
        public void handle(ReleaseAllocatedResources releaseEvent) {
            availableResources.release(releaseEvent.getNumCpus(), releaseEvent.getMemInMb());
            logger.info("Release allocated resources: " + releaseEvent);
        }
    };

    /**
     * Handle the Acknowledge message from a worker
     */
    Handler<Probe.Ack> handleProbeAck = new Handler<Probe.Ack>() {
        @Override
        public void handle(Probe.Ack ack) {

            logger.debug("Got ProbeAck!!!!");
            long jobId = ack.getJobId();
            for (Map.Entry<RequestResource, PendingJob> jobEntry : ongoingJobs.entrySet()) {

                long ongoingJobId = jobEntry.getKey().getId();
                if (ongoingJobId == jobId) {
                    PendingJob pendingJob = jobEntry.getValue();
                    pendingJob.addAck(ack.getSource());
                    handleProbeResponse(jobEntry);
                }
            }
        }
    };

    /**
     * Hnalde the Negative acknowlege message from a worker
     */
    Handler<Probe.Nack> handleProbeNack = new Handler<Probe.Nack>() {
        @Override
        public void handle(Probe.Nack nack) {

            logger.debug("Got ProbeNack!!!!");
            long jobId = nack.getJobId();
            Map.Entry<RequestResource, PendingJob> job = null;
            for (Map.Entry<RequestResource, PendingJob> jobEntry : ongoingJobs.entrySet()) {

                long ongoingJobId = jobEntry.getKey().getId();
                if (ongoingJobId == jobId) {
                    PendingJob pendingJob = jobEntry.getValue();
                    pendingJob.addNack();
                    job = jobEntry;
                }
            }
            handleProbeResponse(job);
        }
    };

    private void handleProbeResponse(Map.Entry<RequestResource, PendingJob> jobEntry) {

        PendingJob pendingJob = jobEntry.getValue();
        RequestResource requestResource = jobEntry.getKey();

        int responseStatus = pendingJob.allResponsesReceived();
        long jobId = requestResource.getId();

        switch (responseStatus) {
            case -1: // All Nack
                //logger.info(jobId + ": All Nack!!!!");
                ongoingJobs.remove(requestResource);
                // No worker acked the probe, try to reschedule the job
                scheduleJob(requestResource);
                break;
            case 0: // Not enough responses
                //logger.debug(jobId + ": Not enough responses!!!!");
                break;
            case 1: // At least one Ack
                //logger.debug(jobId + ": At least one Ack!!!!");
                Address bestWorker = pendingJob.getBestWorker();
                int numCpus = requestResource.getNumCpus();
                int memoryInMbs = requestResource.getMemoryInMbs();
                int timeToHold = requestResource.getTimeToHoldResource();

                RequestResources.Request request = new RequestResources.Request(self, bestWorker, jobId, numCpus, memoryInMbs, timeToHold);
                trigger(request, networkPort);
                break;
            default:
                break;
        }
    }

    /**
     * Schedules a job
     *
     * @param jobEvent Job event
     */
    private void scheduleJob(RequestResource jobEvent) {

        //logger.info("Allocate resources: " + jobEvent.getNumCpus() + " + " + jobEvent.getMemoryInMbs());
        jobList.add(jobEvent);

        long jobId = jobEvent.getId();
        int memoryInMbs = jobEvent.getMemoryInMbs();
        int numCpus = jobEvent.getNumCpus();
        List<Address> addresses = new LinkedList<Address>();

        if (useImprovedSparrow) {
            float ourScore = calculateUtility(availableResources.getNumFreeCpus(), availableResources.getFreeMemInMbs());
            float score = calculateUtility(numCpus, memoryInMbs);
            // calculate all neighbours
            List<Float> scores = new ArrayList();
            for (int i = 0; i < neighbours.size() - 1; i++) {
                float num = calculateUtility(neighbours.get(i).getNumFreeCpus(), neighbours.get(i).getFreeMemoryInMbs());
                if (!scores.contains(num)) {
                    scores.add(num);
                }
            }
            if (scores.size() > 1) {
                if (score >= scores.get(0)) {
                    if (ourScore == scores.get(0)) {
                        logger.info("Sorry, based on our local knowledge, we're the best unsolvable node, drop task" + 
                                ourScore + "/" + score);
                    } else {
                        // forward this job to the top in our neighbours
                        logger.info(self + ":" + ourScore + " Unsolvable locally, forward to: "
                                + neighbours.get(0).getAddress() + ": "
                                + scores.get(0));
                        GradientSearch gs = new GradientSearch(self, neighbours.get(0).getAddress(), jobEvent);
                        trigger(gs, networkPort);
                    }
                } else {
                    // probe best suit peers
                    // logger.info(self + " Solvable locally, probe now " + ourScore);
                    int tempCount = 0;
                    for (int i = 0; i < neighbours.size() && tempCount < nPeersToProbe; i++) {
                        //logger.debug("Sending Probe.Request");
                        if (score <= calculateUtility(neighbours.get(i).getNumFreeCpus(), neighbours.get(i).getFreeMemoryInMbs())) {
                            tempCount++;
                            PeerDescriptor neighbour = neighbours.get(i);
                            addresses.add(neighbour.getAddress());
                            //logger.info(self + ": probe to: " + neighbour.getAddress());
                            Probe.Request request = new Probe.Request(self, neighbour.getAddress(), jobId, numCpus, memoryInMbs);
                            trigger(request, networkPort);
                        }
                    }
                }
            } else if (scores.size() == 1) {

                if (scores.get(0) > score || scores.get(0) == ourScore) {
                    // all equal and larger (exclude us),
                    // or all busy (including us),
                    // maybe initial state or all busy, forward it to others
                    //logger.info("Initial state or too busy" + scores.get(0) + " " + score + " " + ourScore);
                    int tempCount = 0;
                    for (int i = 0; i < neighbours.size() && tempCount < nPeersToProbe; i++) {
                        tempCount++;
                        PeerDescriptor neighbour = neighbours.get(i);
                        addresses.add(neighbour.getAddress());

                        Probe.Request request = new Probe.Request(self, neighbour.getAddress(), jobId, numCpus, memoryInMbs);
                        trigger(request, networkPort);
                    }
                } else if (ourScore > score) {
                    // we are the best in this system maybe?
                    // trigger it to ourself
                    logger.info("Im the best!");
                    Probe.Request request = new Probe.Request(self, self, jobId, numCpus, memoryInMbs);
                    trigger(request, networkPort);
                } else {
                    // 
                    logger.info("Sorry, based on our local knowledge, we're the best unsolvable node, drop task" + 
                            ourScore + "/" + score);
                }
            }
        } else {
            Collections.shuffle(neighbours);
            for (int i = 0; i < neighbours.size() && i < nPeersToProbe; i++) {

                logger.debug("Sending Probe.Request");
                PeerDescriptor neighbour = neighbours.get(i);
                addresses.add(neighbour.getAddress());

                Probe.Request request = new Probe.Request(self, neighbour.getAddress(), jobId, numCpus, memoryInMbs);
                trigger(request, networkPort);
            }
        }

        ongoingJobs.put(jobEvent, new PendingJob(addresses));
    }

    public float calculateUtility(int numFreeCpus, int freeMemoryInMbs) {
        if (freeMemoryInMbs == 0) {
            freeMemoryInMbs = 1;
        }
        return numFreeCpus + (1.0f - 1.0f / freeMemoryInMbs);
    }
}

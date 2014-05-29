package resourcemanager.system.peer.rm;

import common.configuration.RmConfiguration;
import common.helper.UtilityHelper;
import common.peer.AvailableResources;
import common.simulation.RequestResource;
import cyclon.system.peer.cyclon.CyclonSample;
import cyclon.system.peer.cyclon.CyclonSamplePort;
import cyclon.system.peer.cyclon.PeerDescriptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.sics.kompics.ComponentDefinition;
import se.sics.kompics.Handler;
import se.sics.kompics.Positive;
import se.sics.kompics.address.Address;
import se.sics.kompics.network.Network;
import se.sics.kompics.timer.SchedulePeriodicTimeout;
import se.sics.kompics.timer.ScheduleTimeout;
import se.sics.kompics.timer.Timer;
import system.peer.RmPort;
import tman.system.peer.tman.TManSample;
import tman.system.peer.tman.TManSamplePort;

import java.util.*;

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
    Positive<CyclonSamplePort> cyclonSamplePort = positive(CyclonSamplePort.class);
    Positive<TManSamplePort> tmanPort = positive(TManSamplePort.class);

    private Address self;
    private ArrayList<PeerDescriptor> neighbours = new ArrayList<PeerDescriptor>();
    private RmConfiguration configuration;
    private AvailableResources availableResources;
    private Map<RequestResource, PendingJob> ongoingJobs;
    private List<RequestResource> jobList;
    private Random random;

    /**
     * Bind handlers to ports.
     */
    public ResourceManager() {

        subscribe(handleInit, control);
        subscribe(handleCyclonSample, cyclonSamplePort);
        subscribe(handleRequestResource, indexPort);
        subscribe(handleResourceAllocationRequest, networkPort);
        subscribe(handleResourceAllocationResponse, networkPort);
        subscribe(handleProbeRequest, networkPort);
        subscribe(handleTManSample, tmanPort);
        subscribe(handleReservationTimeout, timerPort);
        subscribe(handleAllocationTimeout, timerPort);
        subscribe(handleProbeAck, networkPort);
        subscribe(handleProbeNack, networkPort);
        subscribe(handleGradientSearch, networkPort);
        subscribe(handleAllocateResourcesRequest, networkPort);
        subscribe(handleAllocateResourcesResponse, networkPort);
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
            availableResources = init.getAvailableResources();
            jobList = new ArrayList<RequestResource>();
            long seed = init.getConfiguration().getSeed();
            random = new Random(seed);
            long period = configuration.getPeriod();
            SchedulePeriodicTimeout periodicTimeout = new SchedulePeriodicTimeout(period, period);
            periodicTimeout.setTimeoutEvent(new UpdateTimeout(periodicTimeout));
            trigger(periodicTimeout, timerPort);
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
            if (successfullyAllocatedResources) {
                // Trigger the timeout for holding the resources
                ScheduleTimeout timeout = new ScheduleTimeout(event.getTimeToHold());
                timeout.setTimeoutEvent(new ReleaseAllocatedResources(timeout, event.getNumCpus(), event.getAmountMemInMb()));
                trigger(timeout, timerPort);
            }
            RequestResources.Response resp = new RequestResources.Response(self, event.getSource(), successfullyAllocatedResources, jobId);
            trigger(resp, networkPort);
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
                logger.info("Allocation successful on " + event.getSource() + "for " + event.getJobId());
            } else {
                logger.info("AllocateResources.Response (NACK): " + event.getSource());
                rescheduleJob(event.getJobId());
            }
        }
    };

    /**
     * Schedule incoming resource request.
     */
    Handler<RequestResource> handleRequestResource = new Handler<RequestResource>() {
        @Override
        public void handle(RequestResource jobEvent) {
            logger.info(self + " handleRequestResource Schedule: " + jobEvent.getId());
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
     * Handle the Negative acknowledge message from a worker
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

    /**
     * Handle AllocateResources.Request
     * Try to allocate resources, send back response with result
     */
    Handler<AllocateResources.Request> handleAllocateResourcesRequest = new Handler<AllocateResources.Request>() {

        @Override
        public void handle(AllocateResources.Request request) {

            logger.info("Got AllocateResources.Request");
            long jobId = request.getJobId();
            Address sender = request.getSource();
            int numCpus = request.getNumCpus();
            int amountMemInMb = request.getAmountMemInMb();
            int timeToHold = request.getTimeToHold();

            boolean allocationWasSuccessful = availableResources.allocateIfAvailable(numCpus, amountMemInMb);
            if (allocationWasSuccessful) {
                // Start timer
                ScheduleTimeout timeout = new ScheduleTimeout(timeToHold);
                timeout.setTimeoutEvent(new ReleaseAllocatedResources(timeout, numCpus, amountMemInMb));
                trigger(timeout, timerPort);
            }
            AllocateResources.Response response = new AllocateResources.Response(self, sender, jobId, allocationWasSuccessful);
            trigger(response, networkPort);
        }
    };

    /**
     * Handle AllocateResources.Response
     * Check whether allocation was successful or not -> act accordingly
     */
    Handler<AllocateResources.Response> handleAllocateResourcesResponse = new Handler<AllocateResources.Response>() {

        @Override
        public void handle(AllocateResources.Response response) {

            boolean allocationWasSuccessful = response.wasSuccessful();
            if (allocationWasSuccessful) {
                logger.info("Allocation successful on " + response.getSource() + "for " + response.getJobId());
            } else {
                logger.info("AllocateResources.Response (NACK): " + response.getSource());
                rescheduleJob(response.getJobId());
            }
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
     * Reschedule job with jobId
     * @param jobId Id of the job to reschedule
     */
    private void rescheduleJob(long jobId) {

        for (Map.Entry<RequestResource, PendingJob> jobEntry : ongoingJobs.entrySet()) {

            long ongoingJobId = jobEntry.getKey().getId();
            if (ongoingJobId == jobId) {
                ongoingJobs.remove(jobEntry.getKey());
                scheduleJob(jobEntry.getKey());
                break;
            }
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
        int timeToHoldResource = jobEvent.getTimeToHoldResource();
        List<Address> addresses = new LinkedList<Address>();

        if (useImprovedSparrow) {

            Float ourScore = UtilityHelper.calculateUtility(availableResources.getNumFreeCpus(), availableResources.getFreeMemInMbs());
            Float jobScore = UtilityHelper.calculateUtility(numCpus, memoryInMbs);

            // Calculate all neighbours
            List<Float> scores = new ArrayList();
            for (int i = 0; i < neighbours.size(); i++) {
                PeerDescriptor neighbour = neighbours.get(i);
                Float peerScore = UtilityHelper.calculateUtility(neighbour.getNumFreeCpus(), neighbour.getFreeMemoryInMbs());
                if (!scores.contains(peerScore)) {
                    scores.add(peerScore);
                }
                //logger.info("Peer#" + i + " score: " + peerScore);
            }

            if (scores.size() > 1) {

                Float bestScore = scores.get(0);
                if (jobScore >= bestScore) {

                    if (ourScore.equals(bestScore)) {
                        logger.info("Sorry, based on our local knowledge, we're the best unsolvable node, drop task" +
                                ourScore + "/" + jobScore);
                    } else {
                        // Forward this job to the top in our neighbours
                        PeerDescriptor bestNeighbour = neighbours.get(0);
                        logger.info(self + ":" + ourScore + " Unsolvable locally, forward to: "
                                + bestNeighbour.getAddress() + ": " + bestScore);
                        GradientSearch gs = new GradientSearch(self, bestNeighbour.getAddress(), jobEvent);
                        trigger(gs, networkPort);
                    }

                } else {
                    logger.info("Sending AllocateResources.Request");
                    // Try to allocate resources at best peer
                    // logger.info(self + " Solvable locally, probe now " + ourScore);

                    // Random peer from top half
                    /*
                    for (int i1 = 0; i1 < neighbours.size(); i1++) {
                        PeerDescriptor neighbour = neighbours.get(i1);
                        Float utility = UtilityHelper.calculateUtility(neighbour.getNumFreeCpus(), neighbour.getFreeMemoryInMbs());
                        logger.info("Peer #" + i1 + " = " + utility);
                    }
                    */

                    int i = 0;//random.nextInt((int) Math.ceil(neighbours.size() / 2));

                    Address dest = neighbours.get(i).getAddress();
                    addresses.add(dest);
                    AllocateResources.Request request = new AllocateResources.Request(self, dest, jobId, numCpus, memoryInMbs, timeToHoldResource);
                    trigger(request, networkPort);
                }

            } else if (scores.size() == 1) {
                //logger.info("scores.size() == 1");
                Float bestScore = scores.get(0);
                if (bestScore > jobScore || bestScore.equals(ourScore)) {
                    // All equal and larger (exclude us),
                    // or all busy (including us),
                    // maybe initial state or all busy, forward it to others
                    //logger.info("Initial state or too busy" + scores.get(0) + " " + score + " " + ourScore);
                  int i = 0;//random.nextInt((int) Math.ceil(neighbours.size() / 2));
                  logger.info("FLat gradient: " + jobScore + " -> " + numCpus + ", " + memoryInMbs);
                  Address dest = neighbours.get(i).getAddress();
                  addresses.add(dest);
                  AllocateResources.Request request = new AllocateResources.Request(self, dest, jobId, numCpus, memoryInMbs, timeToHoldResource);
                  trigger(request, networkPort);

                } else if (ourScore > jobScore) {
                    // we are the best in this system maybe?
                    // trigger it to ourself
                    logger.info("Im the best!");
                    Probe.Request request = new Probe.Request(self, self, jobId, numCpus, memoryInMbs);
                    trigger(request, networkPort);
                } else {
                    // 
                    logger.info("Sorry, based on our local knowledge, we're the best unsolvable node, drop task" + 
                            ourScore + "/" + jobScore);
                }
            }

        } else {

            // Use basic Sparrow: shuffle neighbors and send probe
            Collections.shuffle(neighbours);
            if (neighbours.size() == 0) {
                logger.info("Neighbour size 0: " + self);
            }
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
}

package simulator.snapshot;

import common.peer.AvailableResources;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import se.sics.kompics.address.Address;

public class Snapshot {

    private static ConcurrentHashMap<Address, PeerInfo> peers = new ConcurrentHashMap<Address, PeerInfo>();
    private static int counter = 0;
    private static String FILENAME = "search.out";
    private static ConcurrentHashMap<Long, JobTime> jobTracker = new ConcurrentHashMap<Long, JobTime>();


    public static void init(int numOfStripes) {
        FileIO.write("", FILENAME);
    }


    public static void addPeer(Address address, AvailableResources availableResources) {
        peers.put(address, new PeerInfo(availableResources));
    }


    public static void removePeer(Address address) {
        peers.remove(address);
    }



    public static void updateNeighbours(Address address, ArrayList<Address> partners) {
        PeerInfo peerInfo = peers.get(address);

        if (peerInfo == null) {
            return;
        }

        peerInfo.setNeighbours(partners);
    }

    /**
     * Each peer reports in when it schedules a job
     * @param jobId Id of the job
     * @param time Time
     */
    public static void reportJobScheduleTime(long jobId, long time) {
        jobTracker.put(jobId, new JobTime(time));
    }

    /**
     * Each peer reports in when it allocates a job
     * @param jobId Id of the job
     * @param time Time
     */
    public static void reportJobAllocationTime(long jobId, long time) {
        JobTime jobTime = jobTracker.get(jobId);
        jobTime.setEndTime(time);
        jobTracker.put(jobId, jobTime);
    }

    /**
     * Report average time for task to complete
     */
    private static String reportTime() {

        long bestSoFar = 999999999;
        long worstSoFar = -1;
        long sum = 0;

        String str = "Time report" + "\n";
        str += "-----------------------\n";

        List<JobTime> jobTimes = new ArrayList<JobTime>(jobTracker.values());
        Collections.sort(jobTimes);

        for (JobTime jobTime : jobTimes) {
            long finalTime = jobTime.getFinalTime();
            long startTime = jobTime.getStartTime();
            sum += finalTime;

            if (finalTime < bestSoFar) {
                bestSoFar = finalTime;
            }
            if (finalTime > worstSoFar) {
                worstSoFar = finalTime;
            }
            str += startTime + ":" + finalTime + "\n";
        }

        str += "-----------------------\n";
        str += "Best: " + bestSoFar + "\n";
        str += "Worst: " + worstSoFar + "\n";
        str += "\n";
        str +="Average: " + (sum / jobTracker.size()) + "\n";
        str += "-----------------------\n";
        return str;
    }

    public static void report() {

        String str = reportTime();
        System.out.println(str);
        FileIO.append(str, FILENAME);
    }

    public static void report_periodically() {
        String str = "";
        str += "current time: " + counter++ + "\n";
        str += reportNetworkState();
        str += reportDetails();
        str += "###\n";

        System.out.println(str);
        FileIO.append(str, FILENAME);
    }

    private static String reportNetworkState() {
        String str = "---\n";
        int totalNumOfPeers = peers.size();
        str += "total number of peers: " + totalNumOfPeers + "\n";

        return str;
    }


    private static String reportDetails() {
        String str = "---\n";
        int maxFreeCpus = 0;
        int minFreeCpus = Integer.MAX_VALUE;
        int maxFreeMemInMb = 0;
        int minFreeMemInMb = Integer.MAX_VALUE;
        for (PeerInfo p : peers.values()) {
            if (p.getNumFreeCpus() > maxFreeCpus) {
                maxFreeCpus = p.getNumFreeCpus();
            }
            if (p.getNumFreeCpus() < minFreeCpus) {
                minFreeCpus = p.getNumFreeCpus();
            }
            if (p.getFreeMemInMbs() > maxFreeMemInMb) {
                maxFreeMemInMb = p.getFreeMemInMbs();
            }
            if (p.getFreeMemInMbs() < minFreeMemInMb) {
                minFreeMemInMb = p.getFreeMemInMbs();
            }
        }
        str += "Peer with max num of free cpus: " + maxFreeCpus + "\n";
        str += "Peer with min num of free cpus: " + minFreeCpus + "\n";
        str += "Peer with max amount of free mem in MB: " + maxFreeMemInMb + "\n";
        str += "Peer with min amount of free mem in MB: " + minFreeMemInMb + "\n";

        return str;
    }
}

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package common.peer;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * AvailableResources represent the capacity of a worker.
 * It keeps track a worker's resources.
 * @author jdowling
 */
public class AvailableResources {

    private volatile int numFreeCpus;
    private volatile int freeMemInMbs;
    private final int totalCpus;
    private final int totalMemory;
    private List<ReservedResources> reservedResources;

    public AvailableResources(int numFreeCpus, int freeMemInMbs) {
        this.numFreeCpus = numFreeCpus;
        this.freeMemInMbs = freeMemInMbs;
        this.totalCpus = numFreeCpus;
        this.totalMemory = freeMemInMbs;
        this.reservedResources = new ArrayList<ReservedResources>();
    }

    /**
     * Checks if enough resources is available to execute a job
     * @param jobId Id of the job to reserve resources for
     * @param numCpus Number of CPUs required
     * @param memInMbs Required memory
     * @return True if resources are available
     */
    public synchronized boolean reserveIfAvailable(long jobId, int numCpus, int memInMbs) {

        int numReservedCpus = 0;
        int freeReservedMemInMbs = 0;

        for (ReservedResources reservedResource : reservedResources) {
            numReservedCpus += reservedResource.getNumCpus();
            freeReservedMemInMbs += reservedResource.getMemInMb();
        }

        if (numFreeCpus - numReservedCpus >= numCpus && freeMemInMbs - freeReservedMemInMbs >= memInMbs) {

            reservedResources.add(new ReservedResources(jobId, numCpus, memInMbs));
            return true;
        }
        else {
            return false;
        }
    }

    /**
     * Allocates CPU and memory
     * @param jobId Id of the reserved job
     * @return True if resources is successfully allocated
     */
    public synchronized boolean allocate(long jobId) {

        Iterator<ReservedResources> iterator = reservedResources.iterator();
        while (iterator.hasNext()) {
            ReservedResources reservedResource = iterator.next();
            if (reservedResource.getJobId() == jobId) {

                numFreeCpus -= reservedResource.getNumCpus();
                freeMemInMbs -= reservedResource.getMemInMb();
                iterator.remove();
                return true;
            }
        }
        System.out.println("AvailableResources: Could not allocate resources, expired");
        return false;
    }

    /**
     * Releases allocated CPU and memory
     * @param numCpus Number of CPUs
     * @param memInMbs Memory in MB
     */
    public synchronized void release(int numCpus, int memInMbs) {
        if (numCpus <= 0 || memInMbs <= 0) {
            throw new IllegalArgumentException("Invalid numbCpus or mem");
        }
        numFreeCpus += numCpus;
        freeMemInMbs += memInMbs;
    }
    
    public int getNumFreeCpus() {
        return numFreeCpus;
    }

    public int getFreeMemInMbs() {
        return freeMemInMbs;
    }

    /**
     * Determines if a worker got enough resources to execute a job
     * @param numCpus Number of CPUs required
     * @param memory Required memory
     * @return Whether or not the worker could handle such job
     */
    public boolean couldExecuteJob(int numCpus, int memory) {
        return totalCpus >= numCpus && totalMemory >= memory;
    }

    /**
     * Releases the reserved resources of job with jobId
     * @param jobId Id of the job
     */
    public void releaseReservedResources(long jobId) {

        for (ReservedResources resources : reservedResources) {
            if (resources.getJobId() == jobId) {
                reservedResources.remove(resources);
                break;
            }
        }
    }
}
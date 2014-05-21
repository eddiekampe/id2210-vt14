package common.simulation;

import se.sics.kompics.Event;

public final class RequestResource extends Event {
    
    private final long id;
    private final int numCpus;
    private final int memoryInMbs;
    private final int timeToHoldResource;

    public RequestResource(long id, int numCpus, int memoryInMbs, int timeToHoldResource) {
        this.id = id;
        this.numCpus = numCpus;
        this.memoryInMbs = memoryInMbs;
        this.timeToHoldResource = timeToHoldResource;
    }

    public long getId() {
        return id;
    }

    public int getTimeToHoldResource() {
        return timeToHoldResource;
    }

    public int getMemoryInMbs() {
        return memoryInMbs;
    }

    public int getNumCpus() {
        return numCpus;
    }

    @Override
    public boolean equals(Object o) {

        if (this == o) return true;
        if (!(o instanceof RequestResource)) return false;

        RequestResource that = (RequestResource) o;

        return id == that.id;
    }

    @Override
    public int hashCode() {
        return (int) (id ^ (id >>> 32));
    }
}

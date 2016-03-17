package org.queues.ensemble;


public class BatchJob {

    private Object wrappedObject;

    private long creationTime;

    public BatchJob(Object wrappedObject) {
        this.wrappedObject = wrappedObject;
    }

    public Object getWrappedObject() {
        return wrappedObject;
    }

    public long getCreationTime() {
        return creationTime;
    }

    public void setCreationTime(long creationTime) {
        this.creationTime = creationTime;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        BatchJob batchJob = (BatchJob) o;

        return (creationTime != batchJob.creationTime) &&
                (wrappedObject != null ? !wrappedObject.equals(batchJob.wrappedObject) : batchJob.wrappedObject != null);
    }

    @Override
    public int hashCode() {
        int result = wrappedObject != null ? wrappedObject.hashCode() : 0;
        result = 31 * result + (int) (creationTime ^ (creationTime >>> 32));
        return result;
    }

    @Override
    public String toString() {
        return wrappedObject.toString();
    }
}


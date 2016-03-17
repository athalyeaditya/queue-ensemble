package org.queues.ensemble;

import java.util.concurrent.atomic.AtomicLong;

/**
 *
 */
public abstract class AbstractBatchAwareQueue implements BatchAwareQueue {

    protected String queueName;

    /**
     * Queue depth for max number of elements.
     */
    protected int highUnits;

    /**
     * Used for calculating rate metric.
     */
    private final long creationTime;

    /**
     * Number of elements added to this queue from the time it was created.
     */
    private AtomicLong elementsAdded = new AtomicLong(0);

    /**
     * Number of elements drained from this queue from the time it was created.
     */
    private AtomicLong elementsTaken = new AtomicLong(0);

    public AbstractBatchAwareQueue(String queueName, int highUnits) {
        this.queueName = queueName;
        this.highUnits = highUnits;
        this.creationTime = System.currentTimeMillis();
    }

    public abstract BatchJob peek();

    public String getQueueName() {
        return queueName;
    }

    public int getCurrentSize() {
        return size();
    }

    public int getQueueDepth() {
        return highUnits;
    }

    public long getDrainedCount() {
        return elementsTaken.get();
    }

    public long getAddCount() {
        return elementsAdded.get();
    }

    public double getInflowRate() {
        double diff = (System.currentTimeMillis() - creationTime) / 1000;
        return getAddCount() / diff;
    }

    public double getOutflowRate() {
        double diff = (System.currentTimeMillis() - creationTime) / 1000;
        return getDrainedCount() / diff;
    }

    /**
     * Increment batch addition counter.
     */
    protected void incOffer() throws QueueException {
        //Increment element added
        elementsAdded.getAndIncrement();
    }

    /**
     * Increment batch take counter.
     */
    protected void incTake(int takeCount) {
        //Increment element taken
        elementsTaken.getAndAdd(takeCount);
    }


    @Override
    public String toString() {
        return queueName;
    }
}

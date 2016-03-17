package org.queues.spi.impl;

import org.queues.ensemble.AbstractBatchAwareQueue;
import org.queues.spi.BufferCriterion;

/**
 * Simple batch size buffering.
 */
public class SimpleBatchSizeBufferCriterion implements BufferCriterion {

    private int batchSize;

    protected int divisionFactor;

    public SimpleBatchSizeBufferCriterion(int batchSize) {
        this(batchSize, 1);
    }

    /**
     *
     * Division factor added to allow more uniform work distribution among taker threads so that
     * an entire batch is not taken by one thread starving others till next batch criterion is met.
     * @param divisionFactor - Typically division factor will be equal to number of contending readers
     *
     */
    public SimpleBatchSizeBufferCriterion(int batchSize, int divisionFactor) {
        this.batchSize = batchSize;
        this.divisionFactor = divisionFactor;
    }


    @Override
    public <A extends AbstractBatchAwareQueue> boolean isMet(A queue) {
        return queue.size()  >= batchSize / divisionFactor;
    }


    @Override
    public int getDrainCount() {
        return batchSize;
    }
}

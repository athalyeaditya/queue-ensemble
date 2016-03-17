package org.queues.spi.impl;

import org.queues.ensemble.AbstractBatchAwareQueue;
import org.queues.ensemble.BatchJob;

/**
 * Extends {@link SimpleBatchSizeBufferCriterion} with expiry time.
 * So either batch size or expiry (whichever earlier)
 * is used as a criterion before draining the work queue.
 */
public class BatchSizeWithExpiryBufferCriterion extends SimpleBatchSizeBufferCriterion {

    /**
     * Diff in milliseconds for last entry on queue for residual entries to be cleaned up.
     */
    private long expiryDiff;

    public BatchSizeWithExpiryBufferCriterion(int batchSize, long expiryDiff) {
        this(batchSize, 1, expiryDiff);
    }

    public BatchSizeWithExpiryBufferCriterion(int batchSize, int divisionFactor, long expiryDiff) {
        super(batchSize, divisionFactor);
        this.expiryDiff = expiryDiff;
    }

    @Override
    public <A extends AbstractBatchAwareQueue> boolean isMet(A queue) {
        return isExpired(queue) || super.isMet(queue);
    }

    private <A extends AbstractBatchAwareQueue> boolean isExpired(A queue) {
        BatchJob firstJob = queue.peek();
        return firstJob != null && ((queue.size() == 0) || (System.currentTimeMillis() - firstJob.getCreationTime()) >= expiryDiff);
    }
}

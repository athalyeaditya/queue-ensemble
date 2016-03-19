package org.queues.spi;

import org.queues.ensemble.AbstractBatchAwareQueue;
import org.queues.ensemble.SimpleBatchAwareQueue;

/**
 * Define buffer criterion to perform batching to be used by the batch aware queue.
 */
public interface BufferCriterion {

    <A extends AbstractBatchAwareQueue> boolean isMet(A workQueue);

    int getDrainCount();
}

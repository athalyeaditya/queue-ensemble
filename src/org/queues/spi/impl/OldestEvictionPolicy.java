package org.queues.spi.impl;

import org.queues.ensemble.BatchJob;
import org.queues.ensemble.SimpleBatchAwareQueue;
import org.queues.spi.EvictionPolicy;

/**
 * Simplest eviction policy which returns first element
 * of the queue.
 */
public class OldestEvictionPolicy implements EvictionPolicy {

    @Override
    public BatchJob selectElement(SimpleBatchAwareQueue batchedBlockingQueue) {
        //Return first entry
        return batchedBlockingQueue.peek();
    }

}

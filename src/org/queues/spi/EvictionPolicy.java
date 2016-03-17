package org.queues.spi;

import org.queues.ensemble.BatchJob;
import org.queues.ensemble.SimpleBatchAwareQueue;

/**
 * An interface which can be implemented by clients of
 * {@link org.queues.ensemble.SimpleBatchAwareQueue} to
 * decide which element to evict.
 * <p>
 *     Possible implementations
 *     <li>
 *         {@link org.queues.spi.impl.OldestEvictionPolicy}
 *     </li>
 * </p>
 */
public interface EvictionPolicy {

    /**
     * Select an element to evict
     * @param batchAwareQueue - The queue to work with
     * @return The evicted element
     */
    BatchJob selectElement(SimpleBatchAwareQueue batchAwareQueue);
}

package org.queues.ensemble;

import java.util.Collection;

/**
 *
 */
public interface BatchAwareQueue {

    boolean offer(BatchJob batchJob) throws QueueException;

    Collection<BatchJob> take() throws QueueException;

    int size();

    void clear();
}

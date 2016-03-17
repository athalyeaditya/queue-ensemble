package org.queues.ensemble;

import org.queues.annotations.GuardedBy;
import org.queues.annotations.ThreadSafe;
import org.queues.spi.BufferCriterion;
import org.queues.spi.EvictionPolicy;

import java.util.Collection;
import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.BlockingDeque;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Simple blocking implementation of batch aware queue.
 * This queue is thread safe for access (reads/writes)
 * across multiple threads.
 * <p>
 *     This queue allows only one thread to read/write from/to
 *     the queue at a time.
 * </p>
 */
@ThreadSafe
public class SimpleBatchAwareQueue extends AbstractBatchAwareQueue {

    /**
     * Batch lock.
     */
    private final ReentrantLock batchLock = new ReentrantLock();

    /**
     * Internal main queue.
     */
    private BlockingDeque<BatchJob> wrappedQueue = new LinkedBlockingDeque<BatchJob>();


    @GuardedBy("batchLock")
    private BufferCriterion bufferCriterion;

    /**
     * How to evict once high units is reached
     */
    private EvictionPolicy evictionPolicy;

    /**
     * If eviction policy is not null the frequency in milliseconds with which to run eviction thread.
     */
    private int evictionFrequency;

    /**
     * Eviction timer.
     */
    private ScheduledExecutorService evictionService = Executors.newScheduledThreadPool(1, new ThreadFactory() {

        static final String NAME_PREFIX = "Evictor-Thread";

        @Override
        public Thread newThread(Runnable runnable) {
            return new Thread(runnable, NAME_PREFIX);
        }
    });

    /**
     * Condition where takers should wait till batch size is met
     */
    private final Condition batchCondition = batchLock.newCondition();

    /**
     * Condition where put blocks till space is created.
     */
    private final Condition evictCondition = batchLock.newCondition();

    /**
     * Count of total evictions done from this queue.
     */
    private AtomicInteger totalEvictions = new AtomicInteger(0);


    public SimpleBatchAwareQueue(String name, BufferCriterion bufferCriterion) {
        this(name, Integer.MAX_VALUE, bufferCriterion);
    }


    public SimpleBatchAwareQueue(String name, int highUnits, BufferCriterion bufferCriterion) {
        this(name, highUnits, bufferCriterion, null, -1);
    }


    public SimpleBatchAwareQueue(int highUnits, BufferCriterion bufferCriterion, EvictionPolicy evictionPolicy, int evictionFrequency) {
        this("", highUnits, bufferCriterion, evictionPolicy, evictionFrequency);
    }

    public SimpleBatchAwareQueue(String name,
                                 int highUnits,
                                 BufferCriterion bufferCriterion,
                                 EvictionPolicy evictionPolicy,
                                 int evictionFrequency) {
        super(name, highUnits);
        this.bufferCriterion = bufferCriterion;
        this.evictionPolicy = evictionPolicy;
        this.evictionFrequency = evictionFrequency;
        //If bounded schedule evictions
        if ((evictionPolicy != null) && (highUnits < Integer.MAX_VALUE)) {
            scheduleEvictions();
        }
    }

    private void scheduleEvictions() {
        evictionService.scheduleAtFixedRate(new EvictionTask(this), 0L, evictionFrequency, TimeUnit.MILLISECONDS);
    }

    /**
     * Inserts the specified element into this queue, waiting if necessary
     * for space to become available.
     *
     * @param e the element to add
     * @throws InterruptedException     if interrupted while waiting
     * @throws ClassCastException       if the class of the specified element
     *                                  prevents it from being added to this queue
     * @throws NullPointerException     if the specified element is null
     * @throws IllegalArgumentException if some property of the specified
     *                                  element prevents it from being added to this queue
     */

    public void put(Collection<BatchJob> e) throws InterruptedException {
        throw new UnsupportedOperationException("" + e);
    }

    /**
     * Inserts the specified element at the tail of this queue, waiting if
     * necessary up to the specified wait time for space to become available.
     *
     * @return {@code true} if successful, or {@code false} if
     *         the specified waiting time elapses before space is available.
     * @throws NullPointerException {@inheritDoc}
     */

    @GuardedBy("batchLock")
    public boolean offer(BatchJob batchJob, long timeout, TimeUnit unit) throws QueueException {
        boolean isCriterionMet = false;

        final ReentrantLock lock = this.batchLock;
        lock.lock();

        try {
            isCriterionMet = add(batchJob);
            //Signal the consumer to take
            batchCondition.signal();
        } catch (Exception e1) {
            throw new QueueException(e1);
        } finally {
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
        return isCriterionMet;
    }


    @GuardedBy("batchLock")
    public boolean offerFirst(Collection<BatchJob> batchJobs, long timeout, TimeUnit unit) throws QueueException {
        final ReentrantLock lock = this.batchLock;
        lock.lock();

        try {
            BatchJob[] array = batchJobs.toArray(new BatchJob[batchJobs.size()]);
            for (int loop = array.length - 1; loop >= 0; loop--) {
                wrappedQueue.offerFirst(array[loop], timeout, unit);
            }
            //Signal the consumer to take
            batchCondition.signal();
        } catch (Exception e1) {
            throw new QueueException(e1);
        } finally {
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
        return false;
    }

    /**
     * Append to queue and return true is buffer criterion is met, else only append to queue.
     *
     */
    @GuardedBy("batchLock")
    private boolean add(BatchJob batchJob) throws QueueException {
        final ReentrantLock lock = this.batchLock;
        lock.lock();

        try {
            while (evictionPolicy != null && highUnitsMet()) {
                //Wait for timer to evict an entry and create space
                evictCondition.await();
            }
            if (highUnitsMet()) {
                //Same thread to evict
                removeFirst();
            }
            //Set creation time just before adding it
            batchJob.setCreationTime(System.currentTimeMillis());
            wrappedQueue.add(batchJob);
            incOffer();
            return true;
        } catch (Exception e1) {
            throw new QueueException(e1);
        } finally {
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }

    public boolean isBlocking() {
        return true;
    }

    public int getTotalEvictions() {
        return totalEvictions.get();
    }

    @GuardedBy("batchLock")
    private boolean highUnitsMet() {
        return size() >= highUnits;
    }

    public int getEvictionFrequency() {
        return evictionFrequency;
    }

    public BatchJob removeFirst() {
        BatchJob removed = wrappedQueue.removeFirst();
        //Increment evictions counter.
        totalEvictions.getAndIncrement();
        return removed;
    }

    public BatchJob removeLast() {
        return wrappedQueue.removeLast();
    }

    public boolean remove(BatchJob batchJob) {
        return wrappedQueue.remove(batchJob);
    }

    @GuardedBy("batchLock")
    private Collection<BatchJob> takeOrPoll(long timeout, TimeUnit unit) throws QueueException {
        final ReentrantLock lock = this.batchLock;
        lock.lock();

        Queue<BatchJob> batchDrainQueue = new LinkedList<BatchJob>();
        try {
            while (!bufferCriterion.isMet(this)) {
                //Batch size condition not met
                batchCondition.await(timeout, unit);
            }
            //Drain to an internal list
            wrappedQueue.drainTo(batchDrainQueue, bufferCriterion.getDrainCount());
            if (batchDrainQueue.size() > 0) {
                incTake(batchDrainQueue.size());
            }
            //Also signal takers waiting for space to be created
            evictCondition.signalAll();
            return batchDrainQueue;
        } catch (InterruptedException e) {
            throw new QueueException(e);
        } finally {
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }


    public boolean offerFirst(Collection<BatchJob> batchJobs) throws QueueException {
        return offerFirst(batchJobs, -1, TimeUnit.SECONDS);
    }

    /**
     * Retrieves and removes the head of this queue, waiting if necessary
     * until an element becomes available.
     *
     * @return the head of this queue
     * @throws QueueException if interrupted while waiting
     */

    public Collection<BatchJob> take() throws QueueException {
        return takeOrPoll(-1, TimeUnit.SECONDS);
    }

    /**
     * Retrieves and removes the head of this queue, waiting if necessary
     * until an element becomes available.
     *
     * @return the head of this queue
     * @throws QueueException if interrupted while waiting
     */

    public Collection<BatchJob> take(long timeout, TimeUnit unit) throws QueueException {
        return takeOrPoll(timeout, unit);
    }

    /**
     * Retrieves and removes the head of this queue, waiting up to the
     * specified wait time if necessary for an element to become available.
     *
     * @param timeout how long to wait before giving up, in units of
     *                <tt>unit</tt>
     * @param unit    a <tt>TimeUnit</tt> determining how to interpret the
     *                <tt>timeout</tt> parameter
     * @return the head of this queue, or <tt>null</tt> if the
     *         specified waiting time elapses before an element is available
     * @throws QueueException if interrupted while waiting
     */

    public Collection<BatchJob> poll(long timeout, TimeUnit unit) throws QueueException {
        return takeOrPoll(timeout, unit);
    }


    /**
     * Returns the number of elements in this queue.
     *
     * @return the number of elements in this queue
     */

    public int size() {
        //Joining of both items
        return wrappedQueue.size();
    }

    /**
     * Return first element in the queue
     * @return first element
     */
    public BatchJob peek() {
        return wrappedQueue.peek();
    }

    /**
     * Returns an array containing all of the elements in this queue, in
     * proper sequence; the runtime type of the returned array is that of
     * the specified array.  If the queue fits in the specified array, it
     * is returned therein.  Otherwise, a new array is allocated with the
     * runtime type of the specified array and the size of this queue.
     * <p/>
     * <p>If this queue fits in the specified array with room to spare
     * (i.e., the array has more elements than this queue), the element in
     * the array immediately following the end of the queue is set to
     * {@code null}.
     * <p/>
     * <p>Like the {@link #} method, this method acts as bridge between
     * array-based and collection-based APIs.  Further, this method allows
     * precise control over the runtime type of the output array, and may,
     * under certain circumstances, be used to save allocation costs.
     * <p/>
     * <p>Suppose {@code x} is a queue known to contain only strings.
     * The following code can be used to dump the queue into a newly
     * allocated array of {@code String}:
     * <p/>
     * <pre>
     *     String[] y = x.toArray(new String[0]);</pre>
     *
     * Note that {@code toArray(new Object[0])} is identical in function to
     * {@code toArray()}.
     *
     * @param a the array into which the elements of the queue are to
     *          be stored, if it is big enough; otherwise, a new array of the
     *          same runtime type is allocated for this purpose
     * @return an array containing all of the elements in this queue
     * @throws ArrayStoreException  if the runtime type of the specified array
     *                              is not a supertype of the runtime type of every element in
     *                              this queue
     * @throws NullPointerException if the specified array is null
     */

    public BatchJob[] toArray(BatchJob[] a) {
        return wrappedQueue.toArray(a);
    }

    /**
     * Close queue and free any resources.
     */
    public void clear() {
        if (!evictionService.isShutdown()) {
            evictionService.shutdownNow();
        }
        //clean up internal queue
        wrappedQueue.clear();
    }

    /**
     * Inserts the specified element into this queue if it is possible to do
     * so immediately without violating capacity restrictions.
     * When using a capacity-restricted queue, this method is generally
     * preferable to {@link #}, which can fail to insert an element only
     * by throwing an exception.
     *
     * @param batchJob the element to add
     * @return <tt>true</tt> if the element was added to this queue, else
     *         <tt>false</tt>
     * @throws ClassCastException       if the class of the specified element
     *                                  prevents it from being added to this queue
     * @throws NullPointerException     if the specified element is null and
     *                                  this queue does not permit null elements
     * @throws IllegalArgumentException if some property of this element
     *                                  prevents it from being added to this queue
     */

    public boolean offer(BatchJob batchJob) throws QueueException {
        return offer(batchJob, -1, TimeUnit.SECONDS);
    }

    /**
     * Retrieves and removes the head of this queue,
     * or returns <tt>null</tt> if this queue is empty.
     *
     * @return the head of this queue, or <tt>null</tt> if this queue is empty
     */

    public Collection<BatchJob> poll() throws QueueException {
        return take();
    }

    /**
     * Runnable task performing actual eviction from queue
     */
    private class EvictionTask implements Runnable {

        private SimpleBatchAwareQueue workQueue;

        private EvictionTask(SimpleBatchAwareQueue workQueue) {
            this.workQueue = workQueue;
        }

        @Override
        public void run() {
            ReentrantLock lock = batchLock;
            lock.lock();
            //Check for high units
            //If not just come out.
            try {
                if (highUnitsMet()) {
                    //Perform eviction
                    BatchJob evictedJob = evictionPolicy.selectElement(workQueue);

                    totalEvictions.getAndIncrement();
                    //Signal putters
                    evictCondition.signalAll();
                }
            } finally {
                if (lock.isHeldByCurrentThread()) {
                    lock.unlock();
                }
            }
        }
    }
}

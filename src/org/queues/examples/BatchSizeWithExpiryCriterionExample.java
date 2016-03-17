package org.queues.examples;

import org.queues.ensemble.BatchJob;
import org.queues.ensemble.SimpleBatchAwareQueue;
import org.queues.spi.impl.BatchSizeWithExpiryBufferCriterion;
import org.queues.spi.impl.SimpleBatchSizeBufferCriterion;

import java.util.Collection;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

/**
 *
 */
public class BatchSizeWithExpiryCriterionExample {

    static int entryCounter;

    public static void main(String[] r) {
        //Either batch size of 10 or expiry of 5 milliseconds from the first entry, whichever is earlier
        SimpleBatchAwareQueue simpleBatchAwareQueue = new SimpleBatchAwareQueue("My-Queue", 1000, new BatchSizeWithExpiryBufferCriterion(10, 500));

        //Create a pool of writer threads
        ExecutorService writersPool = Executors.newFixedThreadPool(5, new WorkerThreadFactory("Writer-Pool"));
        //Create a pool of reader threads
        ExecutorService readersPool = Executors.newFixedThreadPool(5, new WorkerThreadFactory("Reader-Pool"));

        while (true) {
            writersPool.submit(new Runnable() {
                @Override
                public void run() {
                    try {
                        BatchJob batchJob = new BatchJob(entryCounter++);
                        System.out.printf("Offer by thread [%s] is [%s]", Thread.currentThread().getName(), batchJob);
                        System.out.println();
                        simpleBatchAwareQueue.offer(batchJob);
                        Thread.sleep(50);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            });

            readersPool.submit(new Runnable() {
                @Override
                public void run() {
                    try {
                        Collection<BatchJob> batchJobs = simpleBatchAwareQueue.take();
                        System.out.printf("Collection by thread [%s] is [%s]", Thread.currentThread().getName(), batchJobs);
                        System.out.println();
                        Thread.sleep(500);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            });
        }
    }

    private static class WorkerThreadFactory implements ThreadFactory {

        private String namePrefix;

        public WorkerThreadFactory(String namePrefix) {
            this.namePrefix = namePrefix;
        }

        private AtomicInteger threadCounter = new AtomicInteger(0);

        @Override
        public Thread newThread(Runnable runnable) {
            return new Thread(runnable, namePrefix + "-" + threadCounter.getAndIncrement());
        }
    }
}

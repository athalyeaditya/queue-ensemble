# queue-ensemble

Contains a simple batch-aware queue where multiple threads can write and read in a thread-safe manner.
Differs from standard queue in that a batch of items is read from the queue as opposed to one item.


## Use-Case

This kind of queue can be useful in a scenario where client needs to send messages to a server, but really wants
to send messages in bulk to avoid creating a request per message to be sent.

## Buffering

Buffering criterion can be defined for queue to decide when a **take()** should be allowed. Till such point as the buffering criterion is met, a thread wanting to take items from the queue, will simply come out. 

Following 2 types of buffer criteria are supported.
* Simple batch size only buffering - Take will be allowed when batch size is met in the queue
* Batch size with expiry buffering - Either batch size or expiry of the oldest entry whichever is earlier. Takes may be less than or equal to batch size

## Concept
### Simple Batch Based Queueing
![Simple Batch Size based queueing](https://cloud.githubusercontent.com/assets/7447855/13902851/3b880254-ee83-11e5-8289-40448169c349.png)

As shown in the above diagram, there are multiple writer threads (Writer-1, Writer-2) etc., whereas
Consumer-1, Consumer-2 are reading from the queue. Batch size is set to 5, so unless there are at least 5
entries in the queue, no **take()** is allowed. In the above example Consumer-4 gets all 5 entries.

### Batch With expiry
![Batch With expiry based queueing](https://cloud.githubusercontent.com/assets/7447855/13902881/2bb4cc8e-ee85-11e5-8905-9ebdacbf81f4.png)

Along with batch size expiry time is set to 5 milliseconds. Entry-1 was inserted at T1, Entry-2 at T2 and so on.
When Entry-3 was inserted at T3, T3-T1 >= 5 millisconds and hence even though batch of 5 was not completed,
**take()** was allowed for Consumer-3.

## Eviction

The queue supports an optional eviction behaviour. If queue becomes full, it can evict any entry
to create space, if no takers are available. This can be done in a separate thread. by setting
an eviction policy and eviction frequency. 
Eviction is done by the putter thread is eviction policy is not set.
See API section for more info

# API

## BufferCriterion
An interface which can be extended to use any buffering strategy. 
Custom implementations need to provide **isMet()**

### SimpleBatchSizeBufferCriterion(batchSize)
* `batchSize` : `int` Batch size to be used for take. Taker thread will block till this batch size is met
Each successful **take()** takes number of items = batchsize

### BatchSizeWithExpiryBufferCriterion(batchSize, expiryDiff)
* `batchSize` : `int` Batch size to be used for take
* `expiryDiff` : `long` Number of milliseconds to wait for oldest entry in queue to wait before allowing take
This uses either batchsize or expiry to decide whether to drain for a take. Hence it is possible
that each **take()** may fetch different number of items <= batchsize

``` java
//Create buffering strategy for batchsize 5 or expiry 10 ms
BufferCriterion bufferCriterion = new BatchSizeWithExpiryBufferCriterion(5, 10)
SimpleBatchAwareQueue simpleBatchAwareQueue = new SimpleBatchAwareQueue("My-Queue", 500, bufferCriterion);
```


## SimpleBatchAwareQueue
### SimpleBatchAwareQueue(name, highUnits, bufferCriterion)
* `name` : `String` Queue Name
* `highUnits` : `int` Maximum number of entries that can be kept in the queue.
                If highUnits is reached, putter thread blocks till space is created
* `bufferCriterion` : `BufferCriterion` Buffering criterion to be used. See **Buffering** section above

``` java
//Create a queue with max entries 500 and simple batch buffering with batch size of 10
SimpleBatchAwareQueue simpleBatchAwareQueue = new SimpleBatchAwareQueue("My-Queue", 500, new SimpleBatchSizeBufferCriterion(10));
```
             
### SimpleBatchAwareQueue(name, highUnits, bufferCriterion, evictionPolicy, evictionFrequency)             
* `name` : `String` Queue Name
* `highUnits` : `int` Maximum number of entries that can be kept in the queue.
                If highUnits is reached, putter thread blocks till space is created
* `bufferCriterion` : `BufferCriterion` Buffering criterion to be used. See **Buffering** section above
* `evictionPolicy` : `EvictionPolicy` A simple eviction policy can be specified which returns oldest 
                entry to be evicted based on insertion timestamp.
* `evictionFrequency` : `long` Eviction frequency in milliseconds. Eviction thread will be run with this frequency

``` java
//Create a queue with max entries 500 and simple batch buffering with batch size of 10.
//Eviction policy is set to oldest entry and frequency of 1 min
EvictionPolicy evictionPolicy = new OldestEvictionPolicy();
SimpleBatchAwareQueue simpleBatchAwareQueue = new SimpleBatchAwareQueue("My-Queue", 500, new SimpleBatchSizeBufferCriterion(10), evictionPolicy, 60000L);
```

### Methods          
#### offer(batchJob, timeout, unit)
* `batchJob` : `BatchJob` The new job to insert to queue
* `timeout` : `long` Time to wait for space to be created if queue is full. Applies only if eviction policy is set.
* `unit` : `TimeUnit` Units (ms, seconds etc.)

``` java
SimpleBatchAwareQueue simpleBatchAwareQueue = new SimpleBatchAwareQueue("My-Queue", 500, new SimpleBatchSizeBufferCriterion(10));
simpleBatchAwareQueue.offer(new BatchJob("String-1"), -1, Timeout.Seconds);
simpleBatchAwareQueue.offer(new BatchJob("Int-1"), -1, Timeout.Seconds);
```

#### take()
* `returns` : `Collection<BatchJob>` A Collection of batch jobs if buffer criterion is met. If not met, collection will be empty

``` java
SimpleBatchAwareQueue simpleBatchAwareQueue = new SimpleBatchAwareQueue("My-Queue", 500, new SimpleBatchSizeBufferCriterion(10));
new Thread(new Runnable() {
        public void run() {
           try {
               for (int i = 0, i < 1000; i++) { 
                   BatchJob batchJob = new BatchJob(entryCounter++);
                   System.out.printf("Offer by thread [%s] is [%s]", Thread.currentThread().getName(), batchJob);
                   System.out.println();
                   simpleBatchAwareQueue.offer(batchJob);
                   Thread.sleep(50);
               }
           } catch (Exception e) {
               e.printStackTrace();
           }
       }).start();
       
new Thread(new Runnable() {
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
       }).start()       
```
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
**take()** was allowed for COnsumer-3.

## Eviction

The queue supports an optional eviction behaviour. If queue becomes full, it can evict any entry
to create space, if no takers are available. This is done in a separate thread. An eviction
frequency can be set for the same
See API section for more info

# API

## SimpleBatchAwareQueue
### SimpleBatchAwareQueue(name, highUnits, bufferCriterion)
* `name` : `String` Queue Name
* `highUnits` : `int` Maximum number of entries that can be kept in the queue.
                If highUnits is reached, putter thread blocks till space is created
* `bufferCriterion` : `BufferCriterion` Buffering criterion to be used. See **Buffering** section above
             
### SimpleBatchAwareQueue(name, highUnits, bufferCriterion, evictionPolicy, evictionFrequency)             
* `name` : `String` Queue Name
* `highUnits` : `int` Maximum number of entries that can be kept in the queue.
                If highUnits is reached, putter thread blocks till space is created
* `bufferCriterion` : `BufferCriterion` Buffering criterion to be used. See **Buffering** section above
* `evictionPolicy` : `EvictionPolicy` A simple eviction policy can be specified which returns oldest 
                entry to be evicted based on insertion timestamp.
* `evictionFrequency` : `long` Eviction frequency in milliseconds. Eviction thread will be run with this frequency
              
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



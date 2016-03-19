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



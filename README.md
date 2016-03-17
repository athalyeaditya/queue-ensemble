# queue-ensemble

Contains a simple batch-aware queue where multiple threads can write and read in a thread-safe manner.
Differs from standard queue in that a batch of items is read from the queue as opposed to one item.


# Use-Case

This kind of queue can be useful in a scenario where client needs to send messages to a server, but really wants
to send messages in bulk to avoid creating a request per message to be sent.

# Buffering

Buffering criterion can be defined for queue to decide when a **take** should be allowed. Till such point as the buffering criterion is met, a thread wanting to take items from the queue, will simply come out. 


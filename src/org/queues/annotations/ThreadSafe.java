package org.queues.annotations;

import java.lang.annotation.Documented;
import java.lang.annotation.Target;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * The class to which this annotation is applied is thread-safe.  This means that
 * no sequences of accesses (reads and writes to public fields, calls to public methods)
 * may put the object into an invalid state, regardless of the interleaving of those actions
 * by the runtime, and without requiring any additional synchronization or coordination on the
 * part of the caller.
 *
 **/
@Documented
@Target({ElementType.TYPE, ElementType.FIELD, ElementType.METHOD})
@Retention(RetentionPolicy.SOURCE)
public @interface ThreadSafe {
}

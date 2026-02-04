// In sdk/src/main/java/com/axoid/sdk/BreadcrumbRingBuffer.kt

package com.axoid.sdk

import java.util.Date

/**
 * A thread-safe, fixed-size ring buffer for storing the most recent Breadcrumbs.
 * This class is designed for high performance and concurrency.
 */
class BreadcrumbRingBuffer(private val capacity: Int) {

    // The array holds the breadcrumbs. It is sized to the capacity.
    private val buffer: Array<Breadcrumb?> = arrayOfNulls(capacity)

    // A single integer pointer that indicates the next available slot.
    // It wraps around using the modulo operator.
    @Volatile
    private var head: Int = 0

    // We use a simple object for locking to ensure thread safety during writes.
    private val lock = Any()

    /**
     * Adds a new breadcrumb to the buffer, overwriting the oldest entry if full.
     * This operation is thread-safe.
     */
    fun add(breadcrumb: Breadcrumb) {
        synchronized(lock) {
            buffer[head] = breadcrumb
            head = (head + 1) % capacity
        }
    }

    /**
     * Returns a snapshot of all breadcrumbs currently in the buffer,
     * ordered from oldest to newest. This is crucial for crash reporting.
     * This operation is thread-safe.
     */
    fun getSnapshot(): List<Breadcrumb> {
        val snapshot = mutableListOf<Breadcrumb>()
        synchronized(lock) {
            // The current 'head' is the position of the *next* write, so it's the
            // start of our oldest data. We read from head to the end of the array,
            // then from the beginning of the array up to head.
            for (i in 0 until capacity) {
                val index = (head + i) % capacity
                buffer[index]?.let { snapshot.add(it) }
            }
        }
        return snapshot
    }
}

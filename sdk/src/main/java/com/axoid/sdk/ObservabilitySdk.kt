// In sdk/src/main/java/com/axoid/sdk/ObservabilitySdk.kt

package com.axoid.sdk

import android.content.Context
import java.util.Date

/**
 * The main public entry point for the Axoid Observability SDK.
 */
object ObservabilitySdk {

    private lateinit var ringBuffer: BreadcrumbRingBuffer
    private var isInitialized = false
    private val lock = Any()

    /**
     * Initializes the SDK. This must be called once, typically in the Application's onCreate().
     *
     * @param context The application context.
     * @param breadcrumbCapacity The number of breadcrumbs to keep in memory.
     */
    fun init(context: Context, breadcrumbCapacity: Int = 50) {
        synchronized(lock) {
            if (isInitialized) {
                // You could log a warning here
                return
            }
            ringBuffer = BreadcrumbRingBuffer(breadcrumbCapacity)

            // --- SET UP THE CRASH HANDLER ---
            // 1. Get the current default handler. We need to call it later.
            val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()

            // 2. Create an instance of our custom handler.
            val sdkCrashHandler = SdkCrashHandler(defaultHandler, ringBuffer)

            // 3. Set our custom handler as the new default.
            Thread.setDefaultUncaughtExceptionHandler(sdkCrashHandler)
            // -------------------------------

            isInitialized = true

            // TODO: Set up ANR detection.
            // TODO: Set up network monitoring.
        }
    }

    /**
     * Leaves a breadcrumb to mark a user action or interesting event.
     */
    fun leaveBreadcrumb(type: String, message: String, metadata: Map<String, String> = emptyMap()) {
        if (!isInitialized) return
        val breadcrumb = Breadcrumb(Date(), type, message, metadata)
        ringBuffer.add(breadcrumb)
    }

    /**
     * (For internal use) Gets a snapshot of all current breadcrumbs.
     */
    internal fun getBreadcrumbsSnapshot(): List<Breadcrumb> {
        if (!isInitialized) return emptyList()
        return ringBuffer.getSnapshot()
    }
}

// In sdk/src/main/java/com/axoid/sdk/ObservabilitySdk.kt

package com.axoid.sdk

import android.content.Context
import android.util.Log

import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

/**
 * The main public entry point for the Axoid Observability SDK.
 */
object ObservabilitySdk {

    private const val UPLOAD_WORK_TAG = "axoid_sdk_upload_work" // A unique tag for our work

    private lateinit var ringBuffer: BreadcrumbRingBuffer
    private lateinit var stackTraceTrie: StackTraceTrie // <-- ADD THIS
    private var isInitialized = false
    private val lock = Any()

    private lateinit var reportManager: ReportManager // <-- ADD THIS

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

            // 1. Create the persistence layer
            val triePersistence = TriePersistence(context)
            reportManager = ReportManager(context)
            ringBuffer = BreadcrumbRingBuffer(breadcrumbCapacity)
            stackTraceTrie = StackTraceTrie(triePersistence)

            // --- SET UP THE CRASH HANDLER ---
            // 1. Get the current default handler. We need to call it later.
            val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()

            // 2. Create an instance of our custom handler.
            val sdkCrashHandler = SdkCrashHandler(
                defaultHandler=defaultHandler,
                breadcrumbRingBuffer =ringBuffer,
                stackTraceTrie = stackTraceTrie,
                reportManager
            )

            // 3. Set our custom handler as the new default.
            Thread.setDefaultUncaughtExceptionHandler(sdkCrashHandler)
            // -------------------------------

            isInitialized = true


            // --- SCHEDULE THE UPLOAD WORKER ---
            scheduleBackgroundUploader(context)

            // TODO: Set up ANR detection.
            // TODO: Set up network monitoring.
        }
    }

    /**
     * Leaves a breadcrumb to mark a user action or interesting event.
     */
    fun leaveBreadcrumb(type: String, message: String, metadata: Map<String, String> = emptyMap()) {
        if (!isInitialized) return
        val breadcrumb = Breadcrumb(System.currentTimeMillis(), type, message, metadata)
        ringBuffer.add(breadcrumb)
    }

    /**
     * (For internal use) Gets a snapshot of all current breadcrumbs.
     */
    internal fun getBreadcrumbsSnapshot(): List<Breadcrumb> {
        if (!isInitialized) return emptyList()
        return ringBuffer.getSnapshot()
    }


    private fun scheduleBackgroundUploader(context: Context) {
        // Define constraints for the worker: it needs a network connection.
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        // Create a periodic work request to run approximately every hour.
        // WorkManager may adjust the timing to optimize for battery.
        val uploadWorkRequest = PeriodicWorkRequestBuilder<UploadWorker>(1, TimeUnit.HOURS)
            .setConstraints(constraints)
            .addTag(UPLOAD_WORK_TAG)
            .build()

        // Enqueue the work, keeping any existing work scheduled with the same tag.
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            UPLOAD_WORK_TAG,
            ExistingPeriodicWorkPolicy.KEEP, // Don't re-schedule if it's already running
            uploadWorkRequest
        )
        Log.d("ObservabilitySdk", "Background upload worker scheduled.")
    }
}

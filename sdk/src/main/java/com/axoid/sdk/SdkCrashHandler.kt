// In sdk/src/main/java/com/axoid/sdk/SdkCrashHandler.kt
package com.axoid.sdk

import android.util.Log
import java.io.PrintWriter
import java.io.StringWriter
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone
import java.util.UUID

/**
 * A custom crash handler that intercepts unhandled exceptions.
 * It captures the crash details and the breadcrumb trail, then passes the
 * exception to the original default handler to ensure the app still crashes as expected.
 */
class SdkCrashHandler(
    private val defaultHandler: Thread.UncaughtExceptionHandler?,
    private val breadcrumbRingBuffer: BreadcrumbRingBuffer,
    private val stackTraceTrie: StackTraceTrie,
    private val reportManager: ReportManager
) : Thread.UncaughtExceptionHandler {


    private val logDateFormatter = SimpleDateFormat(
        "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'",
        Locale.US
    ).apply {
        timeZone = TimeZone.getTimeZone("UTC")
    }

    override fun uncaughtException(thread: Thread, throwable: Throwable) {
        // --- THIS IS THE MOMENT OF THE CRASH ---

        // 1. Get a snapshot of the user's last actions (breadcrumbs) from the buffer.
        val breadcrumbs = breadcrumbRingBuffer.getSnapshot()

        // 2. Serialize the stack trace into a string.
        val stackTraceString = getStackTraceAsString(throwable)
        val stackTraceLines = stackTraceString.lines().filter { it.isNotBlank() }

        // 3. ADD THE STACK TRACE TO THE TRIE for aggregation.
        // We will also keep the first set of breadcrumbs associated with this crash.
        // A real SDK might have logic to store breadcrumbs for each occurrence.
        val crashCount = stackTraceTrie.add(stackTraceLines)

        // 4. Create the report payload
        val payload = CrashPayload(
            stackTrace = stackTraceLines,
            breadcrumbs = breadcrumbs,
            // In a more complex system, you'd fetch t w
            // from the Trie's node itself, but for now this is fine.
            firstOccurredAt = System.currentTimeMillis(),
            lastOccurredAt = System.currentTimeMillis(),
            count = crashCount
        )

        // 4. Create the final Report object with HIGH priority
        val report = Report(
            id = UUID.randomUUID().toString(),
            timestamp = System.currentTimeMillis(),
            priority = ReportPriority.HIGH,
            payload = payload
        )

        // 5. SAVE THE REPORT TO DISK instead of logging
        reportManager.saveReport(report)


        // 4. Log the report. In a real scenario, this would be saved to a file.
        Log.e("ObservabilitySdk", "--- CRASH DETECTED. Report saved to disk. ---")
//        Log.e(
//            "ObservabilitySdk", """
//--- CRASH DETECTED & AGGREGATED ---
//This specific crash has now occurred $crashCount times (check the Trie).
//The full report would be batched and sent later. For now, we log it.
//
//BREADCRUMBS (from first occurrence):
//${breadcrumbs.joinToString("\n") { "  - ${logDateFormatter.format(it.timestamp)}: [${it.type}] ${it.message}" }}
//            """.trimIndent()
//        )

        // 4. IMPORTANT: Chain to the original handler.
        // This ensures that the app still behaves as expected (e.g., shows the
        // "App has stopped" dialog) and doesn't just hang.
        defaultHandler?.uncaughtException(thread, throwable)
    }

    private fun getStackTraceAsString(throwable: Throwable): String {
        val stringWriter = StringWriter()
        val printWriter = PrintWriter(stringWriter)
        throwable.printStackTrace(printWriter)
        return stringWriter.toString()
    }
}

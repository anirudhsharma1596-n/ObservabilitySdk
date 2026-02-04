// In sdk/src/main/java/com/axoid/sdk/SdkCrashHandler.kt
package com.axoid.sdk

import android.util.Log
import java.io.PrintWriter
import java.io.StringWriter

/**
 * A custom crash handler that intercepts unhandled exceptions.
 * It captures the crash details and the breadcrumb trail, then passes the
 * exception to the original default handler to ensure the app still crashes as expected.
 */
class SdkCrashHandler(
    private val defaultHandler: Thread.UncaughtExceptionHandler?,
    private val breadcrumbRingBuffer: BreadcrumbRingBuffer
) : Thread.UncaughtExceptionHandler {

    override fun uncaughtException(thread: Thread, throwable: Throwable) {
        // --- THIS IS THE MOMENT OF THE CRASH ---

        // 1. Get a snapshot of the user's last actions (breadcrumbs) from the buffer.
        val breadcrumbs = breadcrumbRingBuffer.getSnapshot()

        // 2. Serialize the stack trace into a string.
        val stackTrace = getStackTraceAsString(throwable)

        // 3. Log everything for now. This is where we will later implement
        //    the Trie aggregation and the Batching & Priority logic.
        Log.e(
            "ObservabilitySdk",
            """
            |--- CRASH DETECTED ---
            |THREAD: ${thread.name}
            |
            |STACK TRACE:
            |$stackTrace
            |
            |BREADCRUMBS (${breadcrumbs.size} items):
            |${breadcrumbs.joinToString("\n") { "  - ${it.timestamp}: [${it.type}] ${it.message}" }}
            |--- END OF REPORT ---
            """.trimMargin()
        )

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

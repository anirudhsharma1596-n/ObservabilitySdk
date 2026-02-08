// In sdk/src/main/java/com/axoid/sdk/UploadWorker.kt
package com.axoid.sdk

import android.Manifest
import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.util.Log
import androidx.annotation.RequiresPermission
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import kotlinx.serialization.json.Json
import java.io.File

/**
 * A background worker that handles the batching and uploading of saved reports.
 * It uses WorkManager to run reliably, even if the app is closed.
 */
class UploadWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    private val reportManager = ReportManager(appContext)

    override suspend fun doWork(): Result {
        Log.d("ObservabilitySdk", "UploadWorker starting...")

        // 1. Load all pending reports from disk
        val pendingReportFiles = reportManager.loadPendingReports()
        if (pendingReportFiles.isEmpty()) {
            Log.d("ObservabilitySdk", "No pending reports to send. Work finished.")
            return Result.success()
        }

        // 2. Check network state and filter reports based on priority
        val reportsToSend = filterReportsByNetwork(pendingReportFiles)
        if (reportsToSend.isEmpty()) {
            Log.d(
                "ObservabilitySdk",
                "Reports are pending, but network conditions are not met. Retrying later."
            )
            // We have reports but can't send them now, so we ask WorkManager to retry.
            return Result.retry()
        }

        // 3. Simulate the upload process
        val success = simulateUpload(reportsToSend)

        // 4. Handle the result
        return if (success) {
            Log.d(
                "ObservabilitySdk",
                "Successfully uploaded ${reportsToSend.size} reports. Cleaning up."
            )
            // On success, delete the uploaded report files
            reportsToSend.forEach { reportManager.deleteReport(it.nameWithoutExtension) }
            Result.success()
        } else {
            Log.d("ObservabilitySdk", "Upload failed. Will retry later.")
            Result.retry()
        }
    }

    @RequiresPermission(Manifest.permission.ACCESS_NETWORK_STATE)
    private fun filterReportsByNetwork(files: List<File>): List<File> {
        val connectivityManager =
            applicationContext.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = connectivityManager.activeNetwork
        val capabilities = connectivityManager.getNetworkCapabilities(network)

        val isUnmetered =
            capabilities?.hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_METERED) == true

        return if (isUnmetered) {
            // On Wi-Fi, send everything
            Log.d(
                "ObservabilitySdk",
                "Network is unmetered (Wi-Fi). Sending all ${files.size} reports."
            )
            files
        } else {
            // On metered network (Cellular), only send HIGH priority reports
            Log.d(
                "ObservabilitySdk",
                "Network is metered (Cellular). Filtering for HIGH priority reports."
            )
            files.filter { file ->
                try {
                    val report: Report = Json.decodeFromString(file.readText())
                    report.priority == ReportPriority.HIGH
                } catch (e: Exception) {
                    false // If a report is malformed, don't send it.
                }
            }
        }
    }

    /**
     * This is a placeholder for a real network call.
     * In a real SDK, you would use a library like Ktor or OkHttp to POST the
     * content of the report files to your backend endpoint.
     */
    private suspend fun simulateUpload(reports: List<File>): Boolean {
        // In a real app, you would serialize the list of reports into a JSON array and send it.
        // For example: `val jsonPayload = Json.encodeToString(reports.map { ... })`
        Log.d("ObservabilitySdk", "Simulating network upload for ${reports.size} reports...")
        // Fake a network delay
        kotlinx.coroutines.delay(2000)

        // Simulate a 90% success rate for demonstration
        val successful = Math.random() > 0.1
        if (successful) {
            Log.d("ObservabilitySdk", "Simulated upload successful.")
        } else {
            Log.d("ObservabilitySdk", "Simulated upload failed.")
        }
        return successful
    }
}

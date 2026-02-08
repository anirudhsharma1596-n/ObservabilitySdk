package com.axoid.sdk


import android.content.Context
import kotlinx.serialization.json.Json
import java.io.File

/**
 * Manages the persistence of crash reports to a file-based queue on disk.
 * Each report is saved as a separate file to ensure durability.
 */
class ReportManager(context: Context) {

    private val reportsDir = File(context.filesDir, "axoid_reports")
    private val storageQuotaBytes = 10 * 1024 * 1024 // 10 MB

    init {
        if (!reportsDir.exists()) {
            reportsDir.mkdirs()
        }
    }

    /**
     * Saves a report to a new file in the reports directory.
     * This is designed to be called from the crash handler and must be fast.
     */
    fun saveReport(report: Report) {
        enforceStorageQuota()
        try {
            val jsonString = Json.encodeToString(report)
            // Use the report's unique ID as the filename.
            val reportFile = File(reportsDir, "${report.id}.json")
            reportFile.writeText(jsonString)
        } catch (e: Exception) {
            // In a real SDK, log this to a separate internal diagnostics log.
            // We must not crash here, as we are already inside the crash handler.
        }
    }

    /**
     * Deletes a report file from disk after it has been successfully sent.
     */
    fun deleteReport(reportId: String) {
        val reportFile = File(reportsDir, "$reportId.json")
        if (reportFile.exists()) {
            reportFile.delete()
        }
    }

    /**
     * Loads all pending reports from the disk. This would be called by the
     * background upload worker.
     */
    fun loadPendingReports(): List<File> {
        return reportsDir.listFiles { _, name -> name.endsWith(".json") }?.toList() ?: emptyList()
    }

    private fun enforceStorageQuota() {
        val files = loadPendingReports().sortedBy { it.lastModified() } // Sort oldest to newest
        var currentSize = files.sumOf { it.length() }

        for (file in files) {
            if (currentSize < storageQuotaBytes) break
            // Delete the oldest files until we are under the quota.
            // A more advanced version would delete low-priority files first.
            val fileSize = file.length()
            if (file.delete()) {
                currentSize -= fileSize
            }
        }
    }
}

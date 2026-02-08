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
}

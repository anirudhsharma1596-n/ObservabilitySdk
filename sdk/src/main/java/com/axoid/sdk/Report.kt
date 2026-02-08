package com.axoid.sdk


import kotlinx.serialization.Serializable

/**
 * Defines the priority of a report. High-priority reports like crashes
 * should be sent immediately, while low-priority logs can wait.
 */
@Serializable
enum class ReportPriority {
    LOW,
    MEDIUM,
    HIGH
}

/**
 * A serializable data class representing a single crash report payload.
 * This contains the aggregated stack trace and the breadcrumbs from the
 * first time the crash was detected.
 */
@Serializable
data class CrashPayload(
    val stackTrace: List<String>,
    val breadcrumbs: List<Breadcrumb>,
    val firstOccurredAt: Long,
    val lastOccurredAt: Long,
    val count: Int
)

/**
 * A generic, serializable wrapper for any report that will be queued for sending.
 *
 * @param id A unique identifier for the report file.
 * @param timestamp When the report was generated.
 * @param priority The priority level of the report.
 * @param payload The actual data, in this case, a CrashPayload.
 */
@Serializable
data class Report(
    val id: String,
    val timestamp: Long,
    val priority: ReportPriority,
    val payload: CrashPayload
)


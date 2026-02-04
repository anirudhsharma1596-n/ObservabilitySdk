// In sdk/src/main/java/com/axoid/sdk/Breadcrumb.kt

package com.axoid.sdk

import java.util.Date

/**
 * Represents a single user action or event.
 *
 * @param timestamp The exact time the event occurred.
 * @param type The category of the event (e.g., "ui.click", "network.request").
 * @param message A descriptive message about the event.
 * @param metadata Additional structured data about the event.
 */
data class Breadcrumb(
    val timestamp: Date,
    val type: String,
    val message: String,
    val metadata: Map<String, String> = emptyMap()
)

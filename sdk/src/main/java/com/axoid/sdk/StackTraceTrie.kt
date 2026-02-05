// In sdk/src/main/java/com/axoid/sdk/StackTraceTrie.kt
package com.axoid.sdk

import kotlinx.serialization.Serializable

/**
 * A node in the StackTraceTrie. Each node represents a single line of a stack trace.
 * It is thread-safe for concurrent modifications.
 */
@Serializable
class TrieNode {
    // The key is the stack trace line (String), the value is the next node.
    val children = mutableMapOf<String, TrieNode>()

    // A counter that is only incremented on the *last* node of a stack trace path.
    // Marked as @Volatile to ensure visibility across threads.
    @Volatile
    var count = 0
}

/**
 * A Trie-based data structure for aggregating and de-duplicating stack traces.
 * This class is designed to be thread-safe.
 */
class StackTraceTrie(private val persistence: TriePersistence) {
    private val root = persistence.loadTrieRoot()
    private val lock = Any()

    /**
     * Adds a full stack trace to the Trie.
     * If the stack trace already exists, it simply increments the count at the leaf node.
     *
     * @param stackTrace The full stack trace, split into a list of lines.
     */
    fun add(stackTrace: List<String>):Int {
        var count=0
        synchronized(lock) {
            var currentNode = root
            for (line in stackTrace) {
                // For each line in the stack trace, we navigate deeper into the Trie.
                // If a path doesn't exist, we create it.
                currentNode = currentNode.children.getOrPut(line) { TrieNode() }
            }
            // Once we reach the end of the path, we increment the crash counter.
            currentNode.count++
            count = currentNode.count
            // --- SAVE TO DISK AFTER MODIFYING ---
            persistence.saveTrieRoot(root)
            return currentNode.count
        }
    }

    /**
     * A placeholder function for future logic. This would be used by the batching
     * system to retrieve aggregated crash reports to be sent to the server.
     */
    fun getAggregatedReports(): Map<List<String>, Int> {
        // This function would traverse the Trie and collect all paths that have a count > 0.
        // For simplicity, we will leave this part for the "Batching & Priority" phase.
        // In a real implementation, it would return a map where the key is the
        // stack trace (List<String>) and the value is the count (Int).
        return emptyMap()
    }
}

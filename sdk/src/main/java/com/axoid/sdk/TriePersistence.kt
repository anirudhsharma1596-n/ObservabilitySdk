package com.axoid.sdk


import android.content.Context
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File

/**
 * Handles saving and loading the StackTraceTrie to/from the device's internal storage.
 */
class TriePersistence(context: Context) {

    private val storageDir = File(context.filesDir, "axoid_sdk_storage")
    private val trieFile = File(storageDir, "stacktrace_trie.json")

    init {
        if (!storageDir.exists()) {
            storageDir.mkdirs()
        }
    }

    /**
     * Loads the Trie's root node from the JSON file.
     * If the file doesn't exist, it returns a new, empty node.
     */
    fun loadTrieRoot(): TrieNode {
        return if (trieFile.exists()) {
            try {
                val jsonString = trieFile.readText()
                Json.decodeFromString<TrieNode>(jsonString)
            } catch (e: Exception) {
                // If deserialization fails (e.g., format change), start fresh
                TrieNode()
            }
        } else {
            TrieNode()
        }
    }

    /**
     * Saves the provided Trie root node to the JSON file.
     */
    fun saveTrieRoot(root: TrieNode) {
        try {
            val jsonString = Json.encodeToString(root)
            trieFile.writeText(jsonString)
        } catch (e: Exception) {
            // Log the error, but don't crash the app
            // In a real SDK, you'd have an internal logger
        }
    }
}

package com.example.pocketpos_lite.core.util

object ErrorUtils {
    fun cleanSupabaseError(message: String?): String {
        val rawMessage = message ?: return "An unknown error occurred"
        
        // Specific Mappings
        if (rawMessage.contains("infinite recursion", ignoreCase = true)) {
            return "Database security error. Please run the updated schema.sql in Supabase."
        }
        if (rawMessage.contains("Bucket not found", ignoreCase = true)) {
            return "Storage Error: The 'logos' or 'product-images' bucket does not exist in Supabase. Please create them in Storage settings."
        }
        
        // Strip technical technical markers
        val technicalMarkers = listOf("URL:", "Headers:", "Http Method:", "at io.github.jan", "StatusCode:", "at ", "body:")
        
        var cleanMessage = rawMessage
        technicalMarkers.forEach { marker ->
            if (cleanMessage.contains(marker)) {
                cleanMessage = cleanMessage.substringBefore(marker).trim()
            }
        }
        
        // Remove technical noise and trailing punctuation
        cleanMessage = cleanMessage.removeSuffix(",").removeSuffix(":").trim()

        return when {
            cleanMessage.contains("invalid_credentials", ignoreCase = true) -> "Invalid email or password"
            cleanMessage.contains("over_email_send_rate_limit", ignoreCase = true) -> "Rate limit exceeded. Please wait 30 seconds."
            cleanMessage.contains("JWT expired", ignoreCase = true) -> "Session expired. Please log in again."
            cleanMessage.isBlank() -> "A database error occurred"
            else -> cleanMessage.substringBefore("\n").trim()
        }
    }
}

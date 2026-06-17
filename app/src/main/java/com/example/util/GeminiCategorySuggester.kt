package com.example.util

import com.example.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Locale

object GeminiCategorySuggester {

    suspend fun suggestCategory(ocrText: String): String = withContext(Dispatchers.Default) {
        if (ocrText.isBlank()) {
            return@withContext "Other"
        }

        val prompt = """
            You are a professional document analysis agent.
            Analyze the following extracted OCR text of a scanned document and suggest the most appropriate single category.
            Choose and return exactly one of the following category names:
            - Financial
            - Legal
            - Personal
            - Academic
            - Invoice
            - Receipt
            - Other
            
            Return ONLY the single word representing the category name (e.g., "Financial" or "Legal"), with absolutely no other text, markdown formatting, symbols, intro, reasoning, or punctuation.
            
            Document OCR Text:
            ${if (ocrText.length > 2000) ocrText.take(2000) else ocrText}
        """.trimIndent()

        // Call Gemini API in a background Dispatchers.IO block
        val response = withContext(Dispatchers.IO) {
            GeminiClient.generateTextContent(prompt)
        }

        val parsedCategory = response.trim().replace(Regex("[^a-zA-Z]"), "")

        // Support validation after trimming and cleaning
        val allowedCategories = setOf("Financial", "Legal", "Personal", "Academic", "Invoice", "Receipt", "Other")
        
        // Find if any allowed category matches (case-insensitive)
        val matchedCategory = allowedCategories.find { 
            it.equals(parsedCategory, ignoreCase = true) 
        }

        if (matchedCategory != null) {
            return@withContext matchedCategory
        }

        // If Gemini returned an API_KEY_ERROR, Exception, or an invalid format, run the resilient rule-based local classifier
        return@withContext performLocalResilientClassification(ocrText)
    }

    private fun performLocalResilientClassification(text: String): String {
        val lowerText = text.lowercase(Locale.getDefault())
        return when {
            // Legal documents
            lowerText.contains("contract") || lowerText.contains("agreement") || lowerText.contains("attorney") || 
            lowerText.contains("court") || lowerText.contains("notary") || lowerText.contains("legal") || 
            lowerText.contains("clause") || lowerText.contains("hereby") || lowerText.contains("signatory") ||
            lowerText.contains("lawyer") || lowerText.contains("liability") -> "Legal"

            // Invoice documents
            lowerText.contains("invoice") || lowerText.contains("billing") || lowerText.contains("bill to") || 
            lowerText.contains("purchase order") || lowerText.contains("payable") -> "Invoice"

            // Receipt documents
            lowerText.contains("receipt") || lowerText.contains("cashier") || lowerText.contains("merchant") || 
            lowerText.contains("store #") || lowerText.contains("subtotal") || lowerText.contains("total due") ||
            lowerText.contains("change due") -> "Receipt"

            // Financial documents (Statements, banks, transactions)
            lowerText.contains("statement") || lowerText.contains("bank") || lowerText.contains("checking") || 
            lowerText.contains("savings") || lowerText.contains("transaction") || lowerText.contains("tax return") || 
            lowerText.contains("investment") || lowerText.contains("credit card") || lowerText.contains("audit") ||
            lowerText.contains("portfolio") || lowerText.contains("revenue") || lowerText.contains("income") -> "Financial"

            // Academic documents
            lowerText.contains("academic") || lowerText.contains("university") || lowerText.contains("school") || 
            lowerText.contains("transcript") || lowerText.contains("degree") || lowerText.contains("diploma") || 
            lowerText.contains("syllabus") || lowerText.contains("curriculum") || lowerText.contains("course") ||
            lowerText.contains("student ID") -> "Academic"

            // Personal documents
            lowerText.contains("personal") || lowerText.contains("private") || lowerText.contains("confidential") || 
            lowerText.contains("medical") || lowerText.contains("diary") || lowerText.contains("my ") || 
            lowerText.contains("passport") || lowerText.contains("driving license") || lowerText.contains("id card") -> "Personal"

            else -> "Other"
        }
    }
}

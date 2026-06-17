package com.example

import com.example.util.GeminiCategorySuggester
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
class ExampleUnitTest {
  @Test
  fun addition_isCorrect() {
    assertEquals(4, 2 + 2)
  }

  @Test
  fun testGeminiCategorySuggester_LocalFallbacks() = runBlocking {
    // Since BuildConfig in unit tests might not have the real API key, 
    // it will execute and hit the performLocalResilientClassification fallback.
    // We can verify each local fallback rule checks out perfectly is correct:

    val legalOcr = "This agreement is signed by the attorney on behalf of the client for our legal binding contract."
    val suggestedLegal = GeminiCategorySuggester.suggestCategory(legalOcr)
    assertEquals("Legal", suggestedLegal)

    val invoiceOcr = "Bill to: Global Tech Corp, Invoice number INV-2026-98, Total payable: $2400.00"
    val suggestedInvoice = GeminiCategorySuggester.suggestCategory(invoiceOcr)
    assertEquals("Invoice", suggestedInvoice)

    val receiptOcr = "Walmart Store #451, CASHIER: Alice, Item Subtotal $12.50, Change due $0.50"
    val suggestedReceipt = GeminiCategorySuggester.suggestCategory(receiptOcr)
    assertEquals("Receipt", suggestedReceipt)

    val financialOcr = "Bank account transaction history statement. Checking balance: $5,023.12"
    val suggestedFinancial = GeminiCategorySuggester.suggestCategory(financialOcr)
    assertEquals("Financial", suggestedFinancial)

    val academicOcr = "University of Science & Tech, student transcript for course grade in Computer Systems"
    val suggestedAcademic = GeminiCategorySuggester.suggestCategory(academicOcr)
    assertEquals("Academic", suggestedAcademic)

    val personalOcr = "Private diary entry or personal confidential letter to family members"
    val suggestedPersonal = GeminiCategorySuggester.suggestCategory(personalOcr)
    assertEquals("Personal", suggestedPersonal)

    val otherOcr = "Some completely unrelated document text that is random"
    val suggestedOther = GeminiCategorySuggester.suggestCategory(otherOcr)
    assertEquals("Other", suggestedOther)
  }
}

package com.example.smartpos.utils

import java.util.Locale

/**
 * Centralized utility for amount parsing and formatting
 * Handles various currency formats and ensures consistent behavior
 */
object AmountUtils {
    
    /**
     * Parse amount string to Double
     * Handles formats like:
     * - "100.00 VND"
     * - "1,234.56"
     * - "1234.56 USD"
     * - "100"
     * 
     * @param amountStr The amount string to parse
     * @return The parsed amount as Double, or 0.0 if parsing fails
     */
    fun parseAmount(amountStr: String): Double {
        if (amountStr.isBlank()) return 0.0
        
        return try {
            // Remove currency codes and any non-numeric characters except . and -
            val cleanedStr = amountStr
                .replace(Regex("[^0-9.\\-]"), "")
                .trim()
            
            cleanedStr.toDoubleOrNull() ?: 0.0
        } catch (e: Exception) {
            0.0
        }
    }
    
    /**
     * Parse amount string with specific currency removal
     * @param amountStr The amount string
     * @param currency The currency code to remove (e.g., "VND", "USD")
     */
    fun parseAmount(amountStr: String, currency: String): Double {
        val withoutCurrency = amountStr.replace(currency, "").trim()
        return parseAmount(withoutCurrency)
    }
    
    /**
     * Format amount with currency
     * Uses Locale.US for consistent decimal separator (period)
     * 
     * @param amount The amount to format
     * @param currency The currency code (default "VND")
     * @param decimals Number of decimal places (default 2)
     * @return Formatted string like "100.00 VND"
     */
    fun formatAmount(amount: Double, currency: String = "VND", decimals: Int = 2): String {
        return String.format(Locale.US, "%.${decimals}f %s", amount, currency)
    }
    
    /**
     * Format amount without currency
     * Uses Locale.US for consistent decimal separator
     * 
     * @param amount The amount to format
     * @param decimals Number of decimal places (default 2)
     * @return Formatted string like "100.00"
     */
    fun formatAmountOnly(amount: Double, decimals: Int = 2): String {
        return String.format(Locale.US, "%.${decimals}f", amount)
    }
    
    /**
     * Calculate total with tip percentage
     * 
     * @param baseAmount The base amount
     * @param tipPercent The tip percentage (0-100)
     * @return Total amount including tip
     */
    fun calculateWithTip(baseAmount: Double, tipPercent: Int): Double {
        return baseAmount + (baseAmount * tipPercent / 100.0)
    }
    
    /**
     * Calculate tip amount from percentage
     * 
     * @param baseAmount The base amount
     * @param tipPercent The tip percentage (0-100)
     * @return The tip amount
     */
    fun calculateTipAmount(baseAmount: Double, tipPercent: Int): Double {
        return baseAmount * tipPercent / 100.0
    }
    
    /**
     * Validate if string is a valid amount
     * @param amountStr The string to validate
     * @return true if valid amount format
     */
    fun isValidAmount(amountStr: String): Boolean {
        if (amountStr.isBlank()) return false
        val cleaned = amountStr.replace(Regex("[^0-9.]"), "")
        val parsed = cleaned.toDoubleOrNull()
        return parsed != null && parsed >= 0
    }
    
    /**
     * Safe division to avoid divide by zero
     * @return result of a/b or 0.0 if b is zero
     */
    fun safeDivide(a: Double, b: Double): Double {
        return if (b == 0.0) 0.0 else a / b
    }
}

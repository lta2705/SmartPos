package com.example.smartpos.utils

import com.example.smartpos.model.EmvCardData
import com.example.smartpos.model.EmvTags
import org.json.JSONObject

/**
 * EMV Message Builder Utility
 * Builds DE55 (EMV data) messages for ISO8583 communication with bank connector
 */
object EmvMessageBuilder {
    
    /**
     * Build DE55 (Field 55) EMV data message
     * Combines card EMV data with transaction information from TCP message
     * 
     * @param emvData EMV card data from NFC
     * @param totTrAmt Transaction amount (TotTrAmt from TCP) - will be converted to 9F02 BCD 6 bytes
     * @param tipAmt Tip amount (TipAmt from TCP) - will be converted to 9F03 BCD 6 bytes
     * @param currCd Currency code (CurrCd from TCP, e.g., "VND") - will be converted to 5F2A BCD 2 bytes
     * @param transactionType Transaction type (TransactionType from TCP) - will be mapped to 9C
     * @param terminalId Terminal ID (TerminalId from TCP) - will be converted to 9F1E ASCII 8 bytes
     * @param transactionDate Transaction date (YYMMDD)
     * @param transactionTime Transaction time (HHMMSS)
     * @return JSON string containing complete EMV message with DE55
     */
    fun buildDE55(
        emvData: EmvCardData,
        totTrAmt: Double,
        tipAmt: Double = 0.0,
        currCd: String = "VND",
        transactionType: String = "SALE",
        terminalId: String,
        transactionDate: String,
        transactionTime: String,
        terminalCountryCode: String = "0704" // Vietnam
    ): String {
        // Build complete tag map including transaction data
        val completeTags = mutableMapOf<String, String>()
        
        // Add all existing card tags
        completeTags.putAll(emvData.rawTlvData)
        
        // Add transaction-specific tags with proper encoding
        
        // 9F02 - Amount Authorized (TotTrAmt) - BCD 6 bytes
        completeTags["9F02"] = convertAmountToBCD6(totTrAmt)
        
        // 9F03 - Amount Other (TipAmt) - BCD 6 bytes
        if (tipAmt > 0) {
            completeTags["9F03"] = convertAmountToBCD6(tipAmt)
        }
        
        // 5F2A - Transaction Currency Code (CurrCd) - BCD 2 bytes
        completeTags["5F2A"] = convertCurrencyCodeToBCD2(currCd)
        
        // 9C - Transaction Type - 1 byte (hex)
        completeTags["9C"] = mapTransactionType(transactionType)
        
        // 9F1E - Terminal ID - ASCII 8 bytes
        completeTags["9F1E"] = convertTerminalIdToASCII8(terminalId)
        
        // 9A - Transaction Date (YYMMDD)
        completeTags["9A"] = transactionDate
        
        // 9F21 - Transaction Time (HHMMSS)
        completeTags["9F21"] = transactionTime
        
        // 9F1A - Terminal Country Code (if not already present)
        if (!completeTags.containsKey("9F1A")) {
            completeTags["9F1A"] = terminalCountryCode
        }
        
        // Build TLV-encoded DE55 field
        val de55Hex = buildTLVHex(completeTags)
        
        // Build JSON message
        val json = JSONObject()
        
        // Add EMV data
        val emvJson = JSONObject()
        emvJson.put("de55", de55Hex)
        emvJson.put("de55Length", de55Hex.length / 2)
        
        // Add parsed fields for reference
        val parsedJson = JSONObject()
        parsedJson.put("pan", emvData.pan ?: "")
        parsedJson.put("cardholderName", emvData.cardholderName ?: "")
        parsedJson.put("expiryDate", emvData.expiryDate ?: "")
        parsedJson.put("amount", totTrAmt)
        parsedJson.put("tipAmount", tipAmt)
        parsedJson.put("currency", currCd)
        parsedJson.put("transactionType", transactionType)
        parsedJson.put("terminalId", terminalId)
        parsedJson.put("transactionDate", transactionDate)
        parsedJson.put("transactionTime", transactionTime)
        parsedJson.put("aid", emvData.aid ?: "")
        parsedJson.put("atc", emvData.atc ?: "")
        
        emvJson.put("parsed", parsedJson)
        
        // Add individual tags for reference
        val tagsJson = JSONObject()
        completeTags.forEach { (tag, value) ->
            tagsJson.put(tag, value)
        }
        emvJson.put("tags", tagsJson)
        
        json.put("emvData", emvJson)
        
        return json.toString()
    }
    
    /**
     * Build TLV-encoded hex string from tag map
     * Format: TAG + LENGTH + VALUE
     */
    private fun buildTLVHex(tags: Map<String, String>): String {
        val sb = StringBuilder()
        
        // Priority order for tags in DE55
        val priorityTags = listOf(
            "4F",   // AID
            "82",   // Application Interchange Profile
            "5A",   // PAN
            "5F20", // Cardholder Name
            "5F24", // Expiry Date
            "5F2A", // Transaction Currency Code
            "5F30", // Service Code
            "9A",   // Transaction Date
            "9C",   // Transaction Type
            "9F02", // Amount Authorized
            "9F03", // Amount Other (Tip)
            "9F1A", // Terminal Country Code
            "9F1E", // Terminal ID
            "9F21", // Transaction Time
            "9F36", // Application Transaction Counter
            "9F10"  // Issuer Application Data
        )
        
        // Add priority tags first
        priorityTags.forEach { tag ->
            tags[tag]?.let { value ->
                sb.append(encodeTLV(tag, value))
            }
        }
        
        // Add remaining tags
        tags.forEach { (tag, value) ->
            if (tag !in priorityTags) {
                sb.append(encodeTLV(tag, value))
            }
        }
        
        return sb.toString()
    }
    
    /**
     * Encode single TLV element: TAG + LENGTH + VALUE
     */
    private fun encodeTLV(tag: String, value: String): String {
        val sb = StringBuilder()
        
        // Add tag (already in hex format)
        sb.append(tag)
        
        // Calculate and add length
        val length = value.length / 2
        sb.append(String.format("%02X", length))
        
        // Add value
        sb.append(value)
        
        return sb.toString()
    }
    
    /**
     * Build ISO8583 message structure
     * For reference - actual ISO8583 packing would be more complex
     */
    fun buildISO8583Message(
        de55: String,
        pan: String?,
        amount: Double,
        transactionDate: String,
        transactionTime: String,
        stan: String, // System Trace Audit Number
        terminalId: String
    ): String {
        val json = JSONObject()
        
        // Message Type Indicator
        json.put("mti", "0200") // Authorization request
        
        // Primary fields
        json.put("de02", pan?.replace(" ", "") ?: "") // PAN
        json.put("de03", "000000") // Processing code (00 = SALE)
        json.put("de04", String.format("%012d", (amount * 100).toLong())) // Amount
        json.put("de07", "$transactionDate$transactionTime") // Transmission date/time
        json.put("de11", stan) // STAN
        json.put("de12", transactionTime) // Local time
        json.put("de13", transactionDate) // Local date
        json.put("de22", "051") // POS Entry Mode (05 = ICC, 1 = PIN entered)
        json.put("de41", terminalId) // Terminal ID
        json.put("de49", "704") // Currency code (VND)
        json.put("de55", de55) // EMV data
        
        return json.toString()
    }
    
    /**
     * Parse DE55 hex string to tag map (for testing/debugging)
     */
    fun parseDE55(de55Hex: String): Map<String, String> {
        val result = mutableMapOf<String, String>()
        var index = 0
        
        while (index < de55Hex.length) {
            try {
                // Read tag (2 or 4 hex chars)
                var tagLength = 2
                if (index + 2 <= de55Hex.length) {
                    val firstByte = de55Hex.substring(index, index + 2).toInt(16)
                    // Check if tag is 2 bytes
                    if ((firstByte and 0x1F) == 0x1F) {
                        tagLength = 4
                    }
                }
                
                if (index + tagLength > de55Hex.length) break
                val tag = de55Hex.substring(index, index + tagLength)
                index += tagLength
                
                // Read length
                if (index + 2 > de55Hex.length) break
                val length = de55Hex.substring(index, index + 2).toInt(16)
                index += 2
                
                // Read value
                val valueLength = length * 2
                if (index + valueLength > de55Hex.length) break
                val value = de55Hex.substring(index, index + valueLength)
                index += valueLength
                
                result[tag] = value
                
            } catch (e: Exception) {
                break
            }
        }
        
        return result
    }
    
    // ============ Conversion Helper Functions ============
    
    /**
     * Convert amount to BCD 6 bytes (12 hex digits)
     * Example: 11000.00 → 000000011000 → \"000000011000\"
     */
    private fun convertAmountToBCD6(amount: Double): String {
        // Convert to cents/smallest unit (no decimal point)
        val amountInCents = (amount * 100).toLong()
        // Format as 12 digits BCD (6 bytes = 12 hex digits)
        return String.format("%012d", amountInCents)
    }
    
    /**
     * Convert currency code to BCD 2 bytes (4 hex digits)
     * Maps currency strings to ISO 4217 numeric codes
     * VND → 704 → \"0704\"
     */
    private fun convertCurrencyCodeToBCD2(currCd: String): String {
        val numericCode = when (currCd.uppercase()) {
            "VND" -> 704
            "USD" -> 840
            "EUR" -> 978
            "GBP" -> 826
            "JPY" -> 392
            "CNY" -> 156
            else -> 704 // Default to VND
        }
        return String.format("%04d", numericCode)
    }
    
    /**
     * Map transaction type string to hex byte
     * SALE → 00, CASH → 01, VOID → 02, REFUND → 20
     */
    private fun mapTransactionType(transactionType: String): String {
        return when (transactionType.uppercase()) {
            "SALE" -> "00"
            "CASH" -> "01"
            "VOID" -> "02"
            "REFUND" -> "20"
            else -> "00" // Default to SALE
        }
    }
    
    /**
     * Convert Terminal ID to ASCII 8 bytes (16 hex chars)
     * Pads or truncates to exactly 8 characters
     * Example: "TERM_0001" → \"5445524D5F30303031\" (hex of ASCII)
     */
    private fun convertTerminalIdToASCII8(terminalId: String): String {
        // Ensure exactly 8 characters
        val paddedId = if (terminalId.length > 8) {
            terminalId.substring(0, 8)
        } else {
            terminalId.padEnd(8, ' ')
        }
        
        // Convert to hex (ASCII encoding)
        return paddedId.toByteArray(Charsets.US_ASCII)
            .joinToString("") { byte -> String.format("%02X", byte) }
    }
}

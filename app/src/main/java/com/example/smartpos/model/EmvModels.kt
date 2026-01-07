package com.example.smartpos.model

import android.util.Log
import org.json.JSONObject

/**
 * EMV Data Models và TLV Parser
 */

/**
 * EMV TLV Tag definitions
 */
object EmvTags {
    // Card Data
    const val TAG_PAN = "5A"                           // Primary Account Number
    const val TAG_CARDHOLDER_NAME = "5F20"              // Cardholder Name
    const val TAG_EXPIRY_DATE = "5F24"                  // Application Expiry Date (YYMMDD)
    const val TAG_PAN_SEQUENCE = "5F34"                 // PAN Sequence Number
    
    // Transaction Data
    const val TAG_AMOUNT_AUTHORIZED = "9F02"            // Amount, Authorized
    const val TAG_AMOUNT_OTHER = "9F03"                 // Amount, Other
    const val TAG_TRANSACTION_CURRENCY = "5F2A"         // Transaction Currency Code
    const val TAG_TRANSACTION_DATE = "9A"               // Transaction Date (YYMMDD)
    const val TAG_TRANSACTION_TYPE = "9C"               // Transaction Type
    const val TAG_TRANSACTION_TIME = "9F21"             // Transaction Time (HHMMSS)
    
    // Terminal Data
    const val TAG_TERMINAL_COUNTRY = "9F1A"             // Terminal Country Code
    const val TAG_TERMINAL_TYPE = "9F35"                // Terminal Type
    const val TAG_TERMINAL_CAPABILITIES = "9F33"        // Terminal Capabilities
    const val TAG_ADDITIONAL_TERMINAL_CAP = "9F40"      // Additional Terminal Capabilities
    const val TAG_TERMINAL_VERIFICATION = "95"          // Terminal Verification Results (TVR)
    
    // Cryptogram and Auth
    const val TAG_APPLICATION_INTERCHANGE_PROFILE = "82" // Application Interchange Profile (AIP)
    const val TAG_CRYPTOGRAM_INFO_DATA = "9F27"         // Cryptogram Information Data
    const val TAG_APP_TRANSACTION_COUNTER = "9F36"      // Application Transaction Counter (ATC)
    const val TAG_APP_CRYPTOGRAM = "9F26"               // Application Cryptogram (AC)
    const val TAG_ISSUER_APP_DATA = "9F10"              // Issuer Application Data (IAD)
    const val TAG_UNPREDICTABLE_NUMBER = "9F37"         // Unpredictable Number
    
    // Card Verification
    const val TAG_CVM_RESULTS = "9F34"                  // CVM Results
    const val TAG_CARD_AUTH_RELATED_DATA = "9F69"       // Card Authentication Related Data
    
    // Application Selection
    const val TAG_AID = "4F"                            // Application Identifier (AID)
    const val TAG_APP_LABEL = "50"                      // Application Label
    const val TAG_APP_PREFERRED_NAME = "9F12"           // Application Preferred Name
    
    // Additional Tags
    const val TAG_TSI = "9B"                            // Transaction Status Information
    const val TAG_DEDICATED_FILE_NAME = "84"            // Dedicated File Name
}

/**
 * EMV Card Data với TLV structure
 */
data class EmvCardData(
    val rawTlvData: Map<String, String>,                // Raw TLV tags
    val pan: String? = null,                            // Primary Account Number
    val cardholderName: String? = null,                 // Cardholder Name
    val expiryDate: String? = null,                     // YYMMDD format
    val panSequence: String? = null,
    val aid: String? = null,                            // Application ID
    val applicationLabel: String? = null,
    val cryptogram: String? = null,                     // Application Cryptogram
    val atc: String? = null,                            // Application Transaction Counter
    val aip: String? = null,                            // Application Interchange Profile
    val tvr: String? = null,                            // Terminal Verification Results
    val tsi: String? = null,                            // Transaction Status Information
    val cvmResults: String? = null,                     // CVM Results
    val iad: String? = null                             // Issuer Application Data
) {
    companion object {
        private const val TAG = "EmvCardData"
        
        /**
         * Parse EMV TLV data from byte array
         */
        fun fromTlvBytes(tlvData: ByteArray): EmvCardData {
            val tlvMap = parseTlv(tlvData)
            
            return EmvCardData(
                rawTlvData = tlvMap,
                pan = tlvMap[EmvTags.TAG_PAN]?.let { bcdToString(hexToBytes(it)) },
                cardholderName = tlvMap[EmvTags.TAG_CARDHOLDER_NAME]?.let { hexToAscii(it) },
                expiryDate = tlvMap[EmvTags.TAG_EXPIRY_DATE],
                panSequence = tlvMap[EmvTags.TAG_PAN_SEQUENCE],
                aid = tlvMap[EmvTags.TAG_AID],
                applicationLabel = tlvMap[EmvTags.TAG_APP_LABEL]?.let { hexToAscii(it) },
                cryptogram = tlvMap[EmvTags.TAG_APP_CRYPTOGRAM],
                atc = tlvMap[EmvTags.TAG_APP_TRANSACTION_COUNTER],
                aip = tlvMap[EmvTags.TAG_APPLICATION_INTERCHANGE_PROFILE],
                tvr = tlvMap[EmvTags.TAG_TERMINAL_VERIFICATION],
                tsi = tlvMap[EmvTags.TAG_TSI],
                cvmResults = tlvMap[EmvTags.TAG_CVM_RESULTS],
                iad = tlvMap[EmvTags.TAG_ISSUER_APP_DATA]
            )
        }
        
        /**
         * Parse TLV byte array to Map
         */
        private fun parseTlv(data: ByteArray): Map<String, String> {
            val result = mutableMapOf<String, String>()
            var index = 0
            
            while (index < data.size) {
                try {
                    // Parse Tag
                    val tagStart = index
                    var tagLength = 1
                    
                    // Check if tag is 2 bytes (first byte & 0x1F == 0x1F)
                    if ((data[index].toInt() and 0x1F) == 0x1F) {
                        tagLength = 2
                    }
                    
                    val tag = bytesToHex(data.sliceArray(tagStart until tagStart + tagLength))
                    index += tagLength
                    
                    // Parse Length
                    val lengthByte = data[index].toInt() and 0xFF
                    index++
                    
                    val length = if (lengthByte and 0x80 == 0x80) {
                        // Multi-byte length
                        val numLengthBytes = lengthByte and 0x7F
                        var calculatedLength = 0
                        for (i in 0 until numLengthBytes) {
                            calculatedLength = (calculatedLength shl 8) or (data[index].toInt() and 0xFF)
                            index++
                        }
                        calculatedLength
                    } else {
                        lengthByte
                    }
                    
                    // Parse Value
                    if (index + length <= data.size) {
                        val value = bytesToHex(data.sliceArray(index until index + length))
                        result[tag] = value
                        index += length
                        
                        Log.d(TAG, "Parsed TLV - Tag: $tag, Length: $length, Value: $value")
                    } else {
                        break
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error parsing TLV at index $index", e)
                    break
                }
            }
            
            return result
        }
        
        /**
         * Convert hex string to bytes
         */
        private fun hexToBytes(hex: String): ByteArray {
            val cleanHex = hex.replace(" ", "")
            return ByteArray(cleanHex.length / 2) { 
                cleanHex.substring(it * 2, it * 2 + 2).toInt(16).toByte() 
            }
        }
        
        /**
         * Convert bytes to hex string
         */
        private fun bytesToHex(bytes: ByteArray): String {
            return bytes.joinToString("") { "%02X".format(it) }
        }
        
        /**
         * Convert BCD (Binary Coded Decimal) to string
         * Used for PAN and other numeric fields
         */
        private fun bcdToString(bytes: ByteArray): String {
            val sb = StringBuilder()
            for (byte in bytes) {
                val high = (byte.toInt() and 0xF0) shr 4
                val low = byte.toInt() and 0x0F
                
                if (high in 0..9) sb.append(high)
                if (low in 0..9) sb.append(low)
            }
            return sb.toString()
        }
        
        /**
         * Convert hex to ASCII string
         */
        private fun hexToAscii(hex: String): String {
            val bytes = hexToBytes(hex)
            return String(bytes, Charsets.US_ASCII).trim()
        }
    }
    
    /**
     * Convert to JSON format for transmission
     */
    fun toJson(): String {
        val json = JSONObject()
        
        // Create parsed object with TLV data
        val parsed = JSONObject()
        val tlv = JSONObject()
        
        rawTlvData.forEach { (tag, value) ->
            tlv.put(tag, value)
        }
        
        parsed.put("tlv", tlv)
        
        // Add parsed human-readable fields
        pan?.let { parsed.put("pan", it) }
        cardholderName?.let { parsed.put("cardholderName", it) }
        expiryDate?.let { parsed.put("expiryDate", formatExpiryDate(it)) }
        aid?.let { parsed.put("aid", it) }
        applicationLabel?.let { parsed.put("applicationLabel", it) }
        
        json.put("parsed", parsed)
        
        return json.toString()
    }
    
    /**
     * Get masked PAN for display
     */
    fun getMaskedPan(): String {
        return if (pan != null && pan.length >= 4) {
            "**** **** **** ${pan.takeLast(4)}"
        } else {
            "**** **** **** ****"
        }
    }
    
    /**
     * Get formatted expiry date (MM/YY)
     */
    fun getFormattedExpiry(): String {
        return expiryDate?.let { formatExpiryDate(it) } ?: "00/00"
    }
    
    /**
     * Format expiry date from YYMMDD to MM/YY
     */
    private fun formatExpiryDate(yymmdd: String): String {
        return if (yymmdd.length >= 4) {
            val mm = yymmdd.substring(2, 4)
            val yy = yymmdd.substring(0, 2)
            "$mm/$yy"
        } else {
            "00/00"
        }
    }
    
    /**
     * Identify card scheme from AID or PAN
     */
    fun getCardScheme(): String {
        // Check AID first
        aid?.let {
            return when {
                it.startsWith("A000000003") -> "VISA"
                it.startsWith("A000000004") -> "MASTERCARD"
                it.startsWith("A000000025") -> "AMEX"
                it.startsWith("A000000065") -> "JCB"
                it.startsWith("A000000333") -> "UNIONPAY"
                else -> "UNKNOWN"
            }
        }
        
        // Fallback to PAN first digit
        pan?.let {
            return when (it.firstOrNull()) {
                '4' -> "VISA"
                '5' -> "MASTERCARD"
                '3' -> "AMEX"
                '6' -> "DISCOVER"
                else -> "UNKNOWN"
            }
        }
        
        return "UNKNOWN"
    }
    
    /**
     * Pack Field 55 (EMV data) for ISO8583 message
     */
    fun packField55(): String {
        val field55Tags = listOf(
            EmvTags.TAG_APPLICATION_INTERCHANGE_PROFILE,
            EmvTags.TAG_APP_TRANSACTION_COUNTER,
            EmvTags.TAG_APP_CRYPTOGRAM,
            EmvTags.TAG_ISSUER_APP_DATA,
            EmvTags.TAG_UNPREDICTABLE_NUMBER,
            EmvTags.TAG_TERMINAL_VERIFICATION,
            EmvTags.TAG_TRANSACTION_DATE,
            EmvTags.TAG_TRANSACTION_TYPE,
            EmvTags.TAG_AMOUNT_AUTHORIZED,
            EmvTags.TAG_TRANSACTION_CURRENCY,
            EmvTags.TAG_TERMINAL_COUNTRY,
            EmvTags.TAG_TSI,
            EmvTags.TAG_CVM_RESULTS,
            EmvTags.TAG_CRYPTOGRAM_INFO_DATA
        )
        
        val sb = StringBuilder()
        
        field55Tags.forEach { tag ->
            rawTlvData[tag]?.let { value ->
                // Append Tag
                sb.append(tag)
                
                // Append Length
                val length = value.length / 2
                sb.append("%02X".format(length))
                
                // Append Value
                sb.append(value)
            }
        }
        
        return sb.toString()
    }
}

/**
 * EMV Transaction Request to Bank Connector
 */
data class EmvTransactionRequest(
    val trmId: String,
    val transactionId: String,
    val amount: String,
    val currency: String = "704", // VND
    val emvData: EmvCardData,
    val field55: String
) {
    fun toJson(): String {
        val json = JSONObject()
        json.put("trmId", trmId)
        json.put("transactionId", transactionId)
        json.put("amount", amount)
        json.put("currency", currency)
        json.put("emvData", JSONObject(emvData.toJson()))
        json.put("field55", field55)
        
        return json.toString()
    }
}

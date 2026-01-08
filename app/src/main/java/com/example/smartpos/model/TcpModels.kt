package com.example.smartpos.model

import org.json.JSONObject

/**
 * TCP Message Models
 */

/**
 * Outgoing TCP Message
 */
data class TcpMessage(
    val msgType: String,
    val trmId: String? = null,
    val status: String? = null,
    val amount: String? = null,
    val transactionId: String? = null,
    val cardData: String? = null,
    val emvData: JSONObject? = null,      // EMV TLV data
    val field55: String? = null,          // Packed Field 55 for ISO8583
    val pcPosId: String? = null           // PC POS ID from server
) {
    fun toJson(): String {
        val jsonObject = JSONObject()
        jsonObject.put("msgType", msgType)
        trmId?.let { jsonObject.put("trmId", it) }
        status?.let { jsonObject.put("status", it) }
        amount?.let { jsonObject.put("amount", it) }
        transactionId?.let { jsonObject.put("transactionId", it) }
        cardData?.let { jsonObject.put("cardData", it) }
        emvData?.let { jsonObject.put("emvData", it) }
        field55?.let { jsonObject.put("field55", it) }
        pcPosId?.let { jsonObject.put("pcPosId", it) }
        return jsonObject.toString()
    }

    companion object {
        // Message Types
        const val MSG_TYPE_INIT = "0"          // Initial connection
        const val MSG_TYPE_HEARTBEAT = 1      // Keep-alive
        const val MSG_TYPE_TRANSACTION = "2"  // Transaction message
        const val MSG_TYPE_RESPONSE = "3"       // Response message

        // Transaction Status
        const val STATUS_STARTED = "STARTED"
        const val STATUS_PROCESSING = "PROCESSING"
        const val STATUS_COMPLETED = "COMPLETED"
        const val STATUS_FAILED = "FAILED"
        const val STATUS_TIMEOUT = "TIMEOUT"

        /**
         * Create initial connection message
         */
        fun createInitMessage(trmId: String): TcpMessage {
            return TcpMessage(
                msgType = MSG_TYPE_INIT,
                trmId = trmId
            )
        }

        /**
         * Create transaction started message
         * @param trmId Terminal ID
         * @param amount Transaction amount
         * @param transactionId Transaction ID from server (optional)
         * @param pcPosId PC POS ID from server (optional)
         */
        fun createTransactionStarted(
            trmId: String,
            amount: String,
            transactionId: String? = null,
            pcPosId: String? = null
        ): TcpMessage {
            return TcpMessage(
                msgType = MSG_TYPE_TRANSACTION,
                trmId = trmId,
                status = STATUS_STARTED,
                amount = amount,
                transactionId = transactionId,
                pcPosId = pcPosId
            )
        }

        /**
         * Create transaction completed message with EMV data
         */
        fun createTransactionCompleted(
            trmId: String,
            transactionId: String,
            emvCardData: EmvCardData
        ): TcpMessage {
            // Convert EMV data to JSON
            val emvJson = JSONObject(emvCardData.toJson())
            
            // Pack Field 55
            val field55 = emvCardData.packField55()
            
            return TcpMessage(
                msgType = MSG_TYPE_TRANSACTION,
                trmId = trmId,
                status = STATUS_COMPLETED,
                transactionId = transactionId,
                emvData = emvJson,
                field55 = field55
            )
        }
        
        /**
         * Create transaction completed message (legacy - for backward compatibility)
         */
        @Deprecated("Use version with EmvCardData instead")
        fun createTransactionCompletedLegacy(
            trmId: String,
            transactionId: String,
            cardData: String
        ): TcpMessage {
            return TcpMessage(
                msgType = MSG_TYPE_TRANSACTION,
                trmId = trmId,
                status = STATUS_COMPLETED,
                transactionId = transactionId,
                cardData = cardData
            )
        }

        /**
         * Create transaction failed message
         */
        fun createTransactionFailed(trmId: String, reason: String): TcpMessage {
            return TcpMessage(
                msgType = MSG_TYPE_TRANSACTION,
                trmId = trmId,
                status = STATUS_FAILED,
                cardData = reason
            )
        }
    }
}

/**
 * Card Data for UI Display (simplified from EMV data)
 */
data class CardData(
    val cardHolderName: String,
    val maskedCardNumber: String,
    val expiryDate: String,
    val cardScheme: String,
    val emvData: EmvCardData? = null       // Optional: full EMV data
) {
    companion object {
        /**
         * Create CardData from EMV data
         */
        fun fromEmvData(emvData: EmvCardData): CardData {
            return CardData(
                cardHolderName = emvData.cardholderName ?: "CARD HOLDER",
                maskedCardNumber = emvData.getMaskedPan(),
                expiryDate = emvData.getFormattedExpiry(),
                cardScheme = emvData.getCardScheme(),
                emvData = emvData
            )
        }
        
        /**
         * Parse pipe-delimited NFC data (legacy support)
         * Example: "JOHN DOE|**** **** **** 1234|12/25|VISA"
         */
        @Deprecated("Use fromEmvData instead for proper EMV handling")
        fun fromNfcData(nfcData: String): CardData? {
            return try {
                val parts = nfcData.split("|")
                if (parts.size >= 4) {
                    CardData(
                        cardHolderName = parts[0].trim(),
                        maskedCardNumber = parts[1].trim(),
                        expiryDate = parts[2].trim(),
                        cardScheme = parts[3].trim().uppercase(),
                        emvData = null
                    )
                } else {
                    null
                }
            } catch (e: Exception) {
                null
            }
        }

        /**
         * Mask card number for display
         * Example: 1234567812345678 -> **** **** **** 5678
         */
        fun maskCardNumber(cardNumber: String): String {
            return if (cardNumber.length >= 4) {
                val lastFour = cardNumber.takeLast(4)
                "**** **** **** $lastFour"
            } else {
                cardNumber
            }
        }
    }

    /**
     * Get card scheme logo resource name
     */
    fun getCardSchemeLogo(): String {
        return when (cardScheme.uppercase()) {
            "VISA" -> "visa"
            "MASTERCARD", "MASTER" -> "mastercard"
            "AMEX", "AMERICAN EXPRESS" -> "amex"
            "DISCOVER" -> "discover"
            "JCB" -> "jcb"
            "UNIONPAY" -> "unionpay"
            else -> "card_default"
        }
    }

    /**
     * Validate expiry date format (MM/YY)
     */
    fun isExpiryValid(): Boolean {
        return try {
            val parts = expiryDate.split("/")
            if (parts.size == 2) {
                val month = parts[0].toIntOrNull() ?: return false
                val year = parts[1].toIntOrNull() ?: return false
                month in 1..12 && year >= 0
            } else {
                false
            }
        } catch (e: Exception) {
            false
        }
    }
}

/**
 * Terminal Configuration
 */
data class TerminalConfig(
    val trmId: String = "TERM_0001",
    val merchantId: String = "MERCHANT_001",
    val storeId: String = "STORE_001",
    val nfcTimeout: Long = 30000L // 30 seconds
)
